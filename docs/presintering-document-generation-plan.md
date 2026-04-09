# Piano funzionale: documento forni alla conferma presinterizzazione (senza codice)

## Obiettivo
Alla pressione di **Conferma Presinterizzazione**:
1. viene salvato il firing (come oggi);
2. viene generato un HTML tramite template documentale;
3. il file HTML viene aperto automaticamente nel browser (come già avviene nel flusso Production).

---

## Punto di partenza già presente nel progetto

### Production (già funzionante)
Il flusso production oggi fa già queste cose:
- costruisce i parametri documento;
- renderizza template HTML;
- salva file temporaneo `.html`;
- apre il file via browser.

Questa parte è già implementata e va riusata come pattern.

### Presintering (stato attuale)
Alla conferma presinterizzazione oggi:
- viene creato il firing;
- vengono collegati gli ordini produzione al firing;
- vengono creati i lotti;
- non viene ancora generato/aperto il documento forni.

---

## Strategia consigliata: “stesso pattern di Production”

### A) Nuovo servizio parametri documento per Presintering
Creare (concettualmente) un servizio gemello di quello production, ad esempio:
- `PresinteringDocumentParamsService`

Responsabilità:
- ricevere il risultato della conferma (firing + pianificazione);
- produrre una `Map<String,Object>` compatibile con template HTML.

### B) Riutilizzare il motore template già esistente
Usare lo stesso motore già usato da production:
- `TemplateEditorService` per `toJson(...)` + `previewTemplate(...)`
- output HTML in file temporaneo
- apertura browser con `DocumentBrowserService`

### C) Punto di trigger
Il trigger giusto è **subito dopo il commit** della conferma presinterizzazione:
- se conferma OK -> genera/apri documento;
- se conferma fallisce -> nessun documento.

---

## Payload minimo richiesto (come da tua richiesta)
Vuoi passare per ogni forno:
- elenco item + quantità;
- temperatura forno.

Payload suggerito:

- `firing`
  - `id`
  - `firing_date`
  - `furnace`
  - `max_temperature`

- `furnace_summary`
  - `name`
  - `temperature`
  - `total_quantity`

- `items`
  - array di record:
    - `item_id`
    - `item_code`
    - `quantity`

- `generated_at`

Nota: siccome la conferma attuale opera su un solo forno per volta, il payload copre già “per forno”.
Se in futuro confermate più forni in batch, basta passare `furnaces: []` invece di una singola testata.

---

## Scelta template (UX semplice)
Per partire “facile facile”:
1. definire un template fisso (es. `PRESINTERING_FURNACE_DOC`);
2. se manca/è vuoto -> mostrare errore utente chiaro e non bloccare il salvataggio firing.

Step successivo (opzionale): aggiungere combo template in UI presintering, come production.

---

## Sequenza operativa proposta
1. Utente clicca “Conferma”.
2. Servizio presintering salva firing + collegamenti + lotti (transazione DB).
3. Se commit riuscito:
   - costruzione parametri documento;
   - render HTML da template;
   - scrittura file temporaneo;
   - apertura browser.
4. Feedback UI:
   - “Presinterizzazione confermata … Documento aperto nel browser: <path>”.

Fallback robusto:
- se generazione/apertura documento fallisce, la conferma DB resta valida;
- mostrare warning non bloccante: “Conferma riuscita, ma documento non generato/aperto”.

---

## Dati necessari da avere disponibili in conferma
Per evitare query aggiuntive inutili, al momento della conferma bisogna avere:
- `firingId`, `firingDate`, `furnaceName`, `maxTemperature`;
- mappa `itemId -> quantity pianificata`;
- risoluzione `itemId -> itemCode`.

Se `itemCode` non è già in memoria, una query mirata sugli item confermati è sufficiente.

---

## Contratto di successo (Definition of Done funzionale)
- Conferma presinterizzazione salva i dati come oggi.
- A conferma riuscita, si apre browser con HTML documento forni.
- Il documento mostra correttamente:
  - forno;
  - temperatura;
  - elenco item;
  - quantità per item.
- In caso errore documento, il salvataggio DB non viene annullato.

---

## Perché questa soluzione è la migliore adesso
- minimo rischio tecnico (riuso pattern production già stabile);
- minima invasività su presintering;
- output immediato utile al reparto (documento forno pronto appena confermi);
- estendibile dopo (multi-forno, template selezionabile, PDF export).
