# Guida alla creazione di una schermata di archivio

## Obiettivo

Una schermata di archivio mostra un elenco di entità salvate, consente di
filtrarle e permette di aprire l'elemento selezionato. Deve comparire subito
dopo il click, mentre i dati vengono caricati senza bloccare il JavaFX
Application Thread.

Gli archivi Template, Composizioni e Modelli Disco costituiscono le
implementazioni di riferimento del progetto.

## Struttura consigliata

Ogni archivio dovrebbe avere almeno questi componenti:

```text
features/<area>/<archivio>/
├── controller/
│   └── ExampleArchiveController.java
├── service/
│   └── ExampleArchiveService.java
└── view/
    └── ExampleArchiveView.java
```

- **View:** crea e aggiorna esclusivamente i controlli JavaFX.
- **Controller:** collega eventi, navigazione e stati della view.
- **Service:** legge i dati e restituisce modelli o DTO privi di controlli
  JavaFX.
- **Navigator:** espone la destinazione senza obbligare la feature a conoscere
  `Stage` o `AppController`.
- **AppController:** compone le dipendenze, mostra la scena e avvia il
  caricamento iniziale.

## Flusso della schermata

Il flusso ideale è:

```text
click sul pulsante Archivio
        ↓
creazione di view e controller
        ↓
visualizzazione immediata della scena
        ↓
stato "Caricamento..."
        ↓
query eseguita dal background executor
        ↓
successo: tabella popolata sul thread JavaFX
oppure
errore: messaggio visibile sul thread JavaFX
```

Il controller non deve eseguire query nel costruttore. Il caricamento parte da
un metodo esplicito `loadInitialData()`, richiamato solamente dopo
`stage.setScene(...)`.

## Responsabilità della view

La view dovrebbe costruire:

- `AppHeader`;
- titolo della pagina;
- campo filtro;
- `TableView`;
- colonne della tabella;
- placeholder iniziale.

La view dovrebbe inoltre esporre tre stati espliciti.

### Caricamento

```java
public void showLoading() {
    table.getItems().clear();
    VBox loadingBox = new VBox(
            8,
            new ProgressIndicator(),
            new Label("Caricamento elementi...")
    );
    loadingBox.setAlignment(Pos.CENTER);
    table.setPlaceholder(loadingBox);
}
```

### Risultato

```java
public void showItems(List<ArchiveRow> rows) {
    table.setItems(FXCollections.observableArrayList(rows));
    table.setPlaceholder(new Label("Nessun elemento trovato."));
}
```

### Errore

```java
public void showLoadError() {
    table.getItems().clear();
    table.setPlaceholder(
            new Label("Errore durante il caricamento degli elementi.")
    );
}
```

Questi metodi modificano controlli JavaFX e devono essere richiamati sul JavaFX
Application Thread. `DebouncedTaskRunner` garantisce questo comportamento per
le callback di successo e fallimento.

## Riga della tabella

È consigliabile rappresentare una riga con un record piccolo e immutabile:

```java
public record ArchiveRow(int id, String name) {
}
```

La tabella non dovrebbe ricevere direttamente oggetti complessi del database se
le servono soltanto identificatore e nome. Il controller può trasformare i
risultati del service in righe dedicate.

## Responsabilità del service

Il service deve:

- validare e normalizzare il filtro;
- interrogare le repository;
- restituire dati indipendenti da JavaFX;
- propagare gli errori tecnici al controller;
- evitare di modificare la view.

Esempio:

```java
public List<Entity> searchEntities(String filter) {
    String normalized = filter == null ? "" : filter.trim().toLowerCase();

    return repository.findAll().stream()
            .filter(entity -> normalized.isBlank()
                    || entity.name().toLowerCase().contains(normalized))
            .toList();
}
```

Quando possibile, il filtro dovrebbe essere eseguito direttamente dal database
per evitare di caricare tutte le righe in memoria. È inoltre necessario evitare
query N+1: un archivio dovrebbe preferibilmente ottenere le righe necessarie con
una singola query mirata.

## Responsabilità del controller

Il controller conserva:

- view;
- service;
- navigator;
- `DebouncedTaskRunner<List<ArchiveRow>>`.

Il costruttore assegna le dipendenze e registra gli eventi, senza caricare dati:

```java
public ExampleArchiveController(ExampleArchiveView view,
                                ExampleArchiveService service,
                                ExampleNavigator navigator,
                                Executor backgroundExecutor) {
    this.view = view;
    this.service = service;
    this.navigator = navigator;
    this.loader = new DebouncedTaskRunner<>(
            backgroundExecutor,
            Duration.millis(300)
    );

    setupActions();
}
```

### Eventi

```java
private void setupActions() {
    view.getFilterField().textProperty().addListener(
            (obs, oldValue, newValue) -> loadDebounced(newValue)
    );

    view.getTable().setOnMouseClicked(event -> {
        ArchiveRow selected = view.getTable()
                .getSelectionModel()
                .getSelectedItem();

        if (selected != null) {
            navigator.showEdit(selected.id());
        }
    });
}
```

### Caricamento iniziale

```java
public void loadInitialData() {
    loader.runNow(
            () -> searchRows(""),
            view::showLoading,
            view::showItems,
            error -> view.showLoadError()
    );
}
```

### Filtro con debounce

```java
private void loadDebounced(String filter) {
    loader.runDebounced(
            () -> searchRows(filter),
            view::showLoading,
            view::showItems,
            error -> view.showLoadError()
    );
}
```

### Conversione in righe

```java
private List<ArchiveRow> searchRows(String filter) {
    return service.searchEntities(filter).stream()
            .map(entity -> new ArchiveRow(entity.id(), entity.name()))
            .toList();
}
```

La funzione passata al runner non deve leggere o modificare controlli JavaFX.
Il testo del filtro viene quindi acquisito prima di avviare il task e passato
come valore al service.

## Composizione in `AppController`

Il router deve conservare questo ordine:

```java
public void showExampleArchive() {
    ExampleArchiveView view = new ExampleArchiveView();
    configureHeader(view.getHeader());

    ExampleArchiveController controller = new ExampleArchiveController(
            view,
            new ExampleArchiveService(app.exampleRepo()),
            this,
            app.backgroundExecutor()
    );

    stage.setScene(createSceneWithCSS(view));
    stage.setTitle("TON - Archivio esempi");
    controller.loadInitialData();
}
```

È importante che `loadInitialData()` sia chiamato dopo `stage.setScene(...)`.
Il metodo deve limitarsi ad avviare il task e restituire subito il controllo al
thread JavaFX, consentendo alla nuova scena di essere disegnata.

## Uso di `DebouncedTaskRunner`

Il runner condiviso offre due modalità:

- `runNow(...)`: avvia immediatamente un caricamento, adatto all'apertura della
  pagina o a un pulsante “Riprova”;
- `runDebounced(...)`: attende un breve periodo senza nuove richieste, adatto a
  un filtro testuale.

Quando arriva una richiesta più recente, il runner:

1. annulla il task precedente quando possibile;
2. incrementa la generazione della richiesta;
3. ignora eventuali risultati appartenenti a generazioni superate;
4. consegna alla view soltanto il risultato più recente.

Il metodo `cancel()` deve essere utilizzato quando viene introdotto un lifecycle
esplicito delle pagine, così una schermata abbandonata non conserva operazioni
inutili.

## Regole di threading

### Sul JavaFX Application Thread

- creare view e controlli;
- leggere i valori correnti dei campi prima di avviare un task;
- cambiare scena;
- mostrare loading, dati ed errori;
- modificare `ObservableList` collegate ai controlli.

### Sul background executor

- eseguire query;
- leggere file;
- trasformare modelli in DTO semplici;
- effettuare elaborazioni potenzialmente lente.

### Da non fare

- modificare `TableView`, `Label`, `VBox` o altri nodi dentro il task;
- creare un nuovo thread per ogni ricerca;
- chiamare `.get()` o `.join()` dal thread JavaFX per attendere un risultato;
- avviare query nel costruttore del controller;
- applicare un risultato senza verificare che sia ancora quello corrente.

## Connessioni al database

L'executor applicativo è attualmente a singolo worker perché le repository del
progetto condividono una connessione JDBC. Prima di aumentare il numero di
worker, occorre adottare una connessione per operazione oppure un pool di
connessioni.

La presenza di un background executor non rende automaticamente sicuro l'uso
concorrente di una singola `Connection`. Una nuova schermata deve quindi usare
l'executor applicativo esistente e non creare autonomamente altri pool per le
query.

## Gestione degli errori

Lo stato di errore deve essere visibile nella pagina. Inoltre, durante lo
sviluppo, è consigliabile registrare l'eccezione ricevuta dalla callback di
fallimento, senza mostrare all'utente stack trace o dettagli SQL.

Per errori recuperabili è possibile aggiungere un pulsante “Riprova” che richiama
`loadInitialData()` oppure una variante che conserva l'ultimo filtro.

## Checklist

Prima di considerare completo un nuovo archivio, verificare che:

- [ ] la view compaia prima dell'inizio della query;
- [ ] il controller non esegua I/O nel costruttore;
- [ ] il caricamento iniziale parta da `loadInitialData()`;
- [ ] le query usino il background executor condiviso;
- [ ] il filtro usi un debounce;
- [ ] richieste superate non possano aggiornare la tabella;
- [ ] siano presenti gli stati loading, vuoto ed errore;
- [ ] soltanto il JavaFX Application Thread modifichi i controlli;
- [ ] il service non dipenda da classi JavaFX;
- [ ] la scena sia impostata prima di `loadInitialData()`;
- [ ] la selezione nulla sia gestita;
- [ ] il titolo della finestra e l'header siano configurati;
- [ ] siano valutate query mirate ed eventuali problemi N+1;
- [ ] siano verificati caricamento iniziale, filtro rapido, errore e navigazione;
- [ ] l'eventuale lifecycle della pagina cancelli il loader quando la schermata
      viene abbandonata.

## Ordine di verifica manuale

1. Aprire l'archivio e verificare che la pagina compaia immediatamente.
2. Verificare che sia visibile lo stato di caricamento.
3. Attendere i dati e verificare righe e placeholder vuoto.
4. Digitare rapidamente nel filtro e verificare che il risultato finale
   corrisponda all'intero testo.
5. Selezionare una riga e verificare la navigazione.
6. Simulare un errore del database e verificare lo stato di errore.
7. Navigare altrove durante un caricamento e controllare che non compaiano
   errori o aggiornamenti sulla schermata corrente.
