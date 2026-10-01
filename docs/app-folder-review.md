# Valutazione della cartella `app`

## Ambito

Questa revisione riguarda `src/main/java/com/orodent/tonv2/app` e il relativo
package `navigation`. La cartella contiene:

- il punto d'ingresso JavaFX (`MainApp`);
- il composition root e contenitore delle dipendenze (`AppContainer`);
- il router/compositore delle schermate (`AppController`);
- la vista iniziale (`HomeView`);
- le interfacce di navigazione esposte alle singole feature.

## Valutazione sintetica

La separazione tra feature e navigazione è una buona base: i controller delle
feature possono dipendere da interfacce piccole (`DocumentsNavigator`,
`LaboratoryNavigator` e `CubageNavigator`) invece che dall'intero
`AppController`. Anche la creazione centralizzata dello header e della scena
evita che ogni feature debba conoscere lo `Stage`.

Il principale limite è che `AppController` ha assunto contemporaneamente i
ruoli di router, composition root e assembler di tutte le feature. Con oltre
cinquecento righe e numerosi costruttori concreti, ogni nuova schermata aumenta
l'accoppiamento e rende difficile collaudare la navigazione senza avviare
JavaFX e il database.

## Problemi rilevati

### Alta priorità

1. **Due destinazioni di cubaggio non sono ancora implementate (mitigato).**
   `showCubageProductFormulaAssignments()` e
   `showCubagePayloadContracts()` non dispongono ancora delle schermate
   dedicate. I pulsanti associati mostrano nel frattempo un messaggio
   “Funzionalità non disponibile”, evitando azioni apparentemente senza effetto.

2. **L'avvio dell'infrastruttura avviene sul thread JavaFX.** `MainApp.start()`
   costruisce `AppController`, che costruisce immediatamente `AppContainer`;
   quest'ultimo avvia il database e apre la connessione prima di mostrare lo
   stage. Il percorso di avvio del database può attendere diversi secondi,
   bloccando la UI e impedendo di presentare uno stato di caricamento o un
   errore recuperabile. Conviene inizializzare l'infrastruttura prima della UI
   oppure in un `Task`, propagando l'esito in modo esplicito.

3. **Lo shutdown può essere richiesto due volte.** `AppController` registra un
   handler su `Stage.setOnCloseRequest`, mentre `MainApp.stop()` richiama di
   nuovo `shutdown()`. Rendere lo shutdown idempotente ridurrebbe il rischio di
   chiudere due volte risorse presenti o future; in alternativa, un solo livello
   dovrebbe possedere il ciclo di vita.

### Media priorità

4. **Ownership delle connessioni non uniforme.** Le repository ricevono una
   connessione condivisa, ma alcuni servizi ricevono nuove connessioni ottenute
   direttamente da `Database`. Dal solo composition root non è evidente chi
   debba chiuderle. È consigliabile esporre un provider/transactor con contratto
   di ownership chiaro, evitando anche l'accesso diretto al campo `database`.

5. **`AppController` è difficile da testare in isolamento.** Il costruttore
   crea concretamente `AppContainer`, carica il CSS, naviga alla home e mostra
   lo stage. L'iniezione di `AppContainer` (o di un'interfaccia di dipendenze),
   insieme alla separazione tra costruzione e `start()`, consentirebbe test di
   navigazione e lifecycle con sostituti controllati.

6. **Assemblaggio duplicato.** `BatchProductionDocumentParamsService` viene
   composto in più metodi con lo stesso insieme di repository. Spostare questi
   factory method in un assembler dedicato o nel container elimina il rischio
   che le configurazioni divergano.

### Bassa priorità

7. **API della scena troppo generica.** `createSceneWithCSS` accetta `Object`
   e lo converte a `Parent` a runtime. Accettare direttamente `Parent` rende il
   contratto verificabile dal compilatore.

8. **Home vuota.** `HomeView` aggiunge un `VBox` senza contenuto. Se la home è
   intenzionalmente un contenitore futuro, un placeholder visibile renderebbe
   più chiaro lo stato della schermata; altrimenti il nodo può essere rimosso.

9. **Visibilità e naming migliorabili.** Il campo `database` è `protected` ma
   viene usato come dettaglio interno del package; inoltre le abbreviazioni
   `app` e `*Repo` rendono il composition root meno auto-documentante. Getter
   mirati e nomi completi migliorerebbero l'incapsulamento.

## Aspetti positivi

- Le interfacce di navigazione sono suddivise per area funzionale.
- Ogni cambio schermata ricrea view e controller, evitando stato UI residuo.
- Header, foglio di stile globale e titoli sono configurati centralmente.
- Le dipendenze condivise sono raccolte in un unico contenitore anziché essere
  create casualmente dentro le view.
- Il fallback dall'editor di un template inesistente riporta all'archivio.

## Piano di intervento suggerito

1. Implementare le due destinazioni di cubaggio attualmente coperte dall'avviso
   temporaneo e rendere idempotente lo shutdown.
2. Introdurre un costruttore di `AppController` che riceva le dipendenze e
   separare la configurazione dalla visualizzazione dello stage.
3. Estrarre assembler/factory per Documents, Laboratory, Registers e Cubage,
   lasciando ad `AppController` soltanto la scelta della destinazione.
4. Uniformare il lifecycle delle connessioni tramite un provider o unit of work.
5. Aggiungere test per mapping dei pulsanti, fallback di navigazione e doppio
   shutdown; successivamente aggiungere smoke test JavaFX per le scene.

## Primo intervento asincrono

L'Archivio Composizioni è stato scelto come feature pilota e lo stesso modello è
stato applicato agli archivi Modelli Disco e Template. Le schermate vengono
mostrate prima di avviare la query, presentano uno stato di caricamento e
applicano i risultati sul thread JavaFX. Le ricerche dei filtri sono ritardate
brevemente e il risultato di una richiesta superata viene ignorato. La gestione
di task, debounce, cancellazione e risultati obsoleti è raccolta nel componente
riutilizzabile `DebouncedTaskRunner`. Gli archivi e il servizio Template usano
ora un `ConnectionProvider`: ogni operazione apre una connessione dedicata e la
chiude al termine. Le operazioni migrate possono così usare il pool di worker
senza condividere la stessa sessione JDBC. Tutte le operazioni applicative
migrate aprono ormai connessioni scoped; il contenitore non conserva più una
sessione JDBC condivisa.

La prima migrazione successiva agli archivi riguarda Registri: suggerimenti e
ricerca completa vengono eseguiti in background con connessioni scoped. La
pagina espone inoltre un cleanup che cancella i loader quando si cambia scena;
anche la generazione del documento composizione viene eseguita in background e
costruisce tutte le repository dentro una connessione scoped. Il pulsante del
documento firing resta disabilitato finché la funzione non verrà implementata.

Produzione Batch carica apertura pagina, linee, template, prodotti e item tramite
loader cancellabili e connessioni scoped. Anche il salvataggio viene eseguito in
background: testata e righe dell'ordine condividono una connessione e una singola
transazione con commit o rollback. La generazione del documento avviene dopo il
commit e segnala separatamente un proprio eventuale errore.

Presinterizzazione esegue in background apertura pagina, suggerimenti del forno e
conferma del piano. Le letture usano connessioni scoped, mentre firing, lotti e
allocazioni vengono salvati atomicamente con `withTransaction`; la generazione
del documento successiva al commit espone separatamente un eventuale errore.

Il salvataggio dei Programmi di cottura viene eseguito in background e crea il
programma e tutti i suoi step nella stessa connessione transazionale scoped.

Setup Item carica i prodotti e gestisce attivazione composizione e creazione item
in background. I due comandi usano transazioni scoped indipendenti.

L'editor Modello Disco mostra subito la scena, carica le versioni esistenti in
background e salva modello, layer, fasce e associazioni in una singola transazione.

L'editor Composizione carica dati iniziali, linee e versioni in background; anche
il salvataggio usa una connessione scoped e preserva il lifecycle del dirty state.

La Gestione Calcoli Cubaggio carica set, payload, versioni e anteprime in
background e salva ogni nuova versione in una transazione scoped. Il controller
annulla i loader quando si lascia la pagina e la view espone stati espliciti di
caricamento, validazione, salvataggio ed errore.

La vecchia importazione inventario basata su CSV è stata rimossa insieme alla
schermata Inventario: `CsvPathsLoader`, `CsvPaths`, `CsvParser`,
`MagazzinoCsvParser` e la relativa voce di navigazione non facevano più parte del
flusso applicativo corrente.

L'editor Template mostra ora la scena prima di caricare preset e contenuto, esegue
query SQL e salvataggi in background e annulla i task quando si lascia la pagina.
I servizi parametri dei preset aprono connessioni scoped; questo ha permesso di
rimuovere definitivamente la connessione JDBC condivisa e le repository legacy
da `AppContainer`.

Anche il bootstrap è asincrono: `MainApp` mostra subito una scena di avvio e
inizializza Derby sul pool di background, offrendo retry e chiusura in caso di
errore. `Database` espone uno stato esplicito, propaga i fallimenti, usa una home
portabile/configurabile e impedisce l'apertura di connessioni prima dello stato
`READY`; avvio e shutdown sono ora responsabilità di `AppContainer`.

La pianificazione locale di Presinterizzazione usa infine un autosave asincrono
con debounce: le modifiche ravvicinate vengono consolidate, la cancellazione del
piano precede il successivo reload e l'ultimo snapshot viene accodato prima del
cleanup. Lo shutdown dell'executor è diventato graceful per consentire il flush
delle operazioni brevi già accodate, con fallback forzato dopo il timeout.

## Giudizio complessivo

La cartella è funzionale come composition root di un'applicazione desktop di
dimensioni contenute e mostra una buona direzione nella separazione delle
interfacce di navigazione. Per la dimensione attuale del progetto, tuttavia,
`AppController` è già oltre la soglia in cui aggiungere feature rimane semplice:
la priorità dovrebbe essere correggere i percorsi senza effetto e chiarire il
lifecycle, poi distribuire l'assemblaggio delle feature in componenti dedicati.

## Editor di codice incorporato

L'integrazione CodeMirror è ora un componente infrastrutturale in `core/ui/editor`
e non appartiene più alla feature Template. Linguaggi, caricamento delle risorse,
composizione della pagina HTML e codifica sicura delle stringhe JavaScript hanno
responsabilità separate. `TemplateEditorView` mantiene privati gli editor concreti
e offre al controller soltanto operazioni semantiche su template e query; in
questo modo un futuro cambio del motore di editing non si propaga nel workflow o
nel controller della feature.
