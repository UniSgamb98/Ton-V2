# Proposta funzionale/DB: integrazione fase di Cubatura (allineata al feedback)

## Obiettivo
Introdurre la cubatura come estensione del flusso esistente, collegando il firing ai dati necessari per etichetta (volume, densità, calcoli), senza introdurre stati applicativi nel DB.

---

## Decisioni recepite
In base al tuo feedback:
- tabella `cubage` **minima** con soli campi: `id`, `firing_id`;
- niente `status`, niente `operator`, niente `notes`, niente audit;
- il concetto di draft viene gestito **fuori DB** con file JSON locale;
- quando il cubage è definitivo, si salva su DB.

---

## Stato attuale su cui ci appoggiamo
Nel DB attuale:
- `production_order_line` contiene `item_id` e `quantity` (quindi un item può avere quantità 30, 50, ecc.);
- `firing` è già collegato alla produzione tramite `production_order_firing`;
- `firing` contiene `max_temperature`, utile per il contesto di cubatura.

Quindi il flusso naturale rimane:
`production_order (+ linee item/quantità) -> firing -> cubage -> misure per singola unità`.

---

## Modello dati consigliato (semplice)

### 1) `cubage` (testata minima)
Campi:
- `id` (PK)
- `firing_id` (FK -> `firing.id`)

Scopo: identificare la sessione di cubatura per un firing.

> Nota: se vuoi obbligare 1 cubage per firing, aggiungi `UNIQUE(firing_id)`.

### 2) `cubage_measurement` (dettaglio per singola unità)
Questa è la parte chiave per il tuo caso: quantità item > 1 e misura per unità.

Campi minimi:
- `id` (PK)
- `cubage_id` (FK -> `cubage.id`)
- `item_id` (FK -> `item.id`)
- `unit_index` (numero progressivo unità: 1..quantity)
- `volume_mm3`
- `density_g_cm3`

Campi utili opzionali per etichetta/calcoli:
- `mass_g`
- altri valori derivati che vuoi stampare in etichetta
- `firing_temperature_snapshot` (copia di `firing.max_temperature` al momento del salvataggio)

Vincolo consigliato:
- `UNIQUE(cubage_id, item_id, unit_index)`
  (garantisce una sola misurazione per quella specifica unità).

---

## Come gestire draft locale JSON (senza status DB)
1. L’app di appoggio carica dati da DB (firing + item + quantity).
2. Durante il lavoro salva/aggiorna un file JSON locale (draft).
3. Al termine, esegue un “publish”:
   - crea/usa `cubage` (`id`, `firing_id`);
   - inserisce tutte le righe in `cubage_measurement`.
4. Da quel momento la fonte ufficiale è il DB.

In questo modo eviti complessità di stati nel database e tieni il DB pulito solo con dati finali.

---

## Chiarimento sul tuo caso (item con quantità 30)
Se hai:
- `item_id = 123`
- `quantity = 30`

allora in cubatura devi inserire **30 righe** in `cubage_measurement` per quello stesso item, con `unit_index` da 1 a 30.

Esempio logico:
- (item 123, unit_index 1) -> volume/densità...
- (item 123, unit_index 2) -> volume/densità...
- ...
- (item 123, unit_index 30) -> volume/densità...

Questo risponde al requisito “cubaggio per ogni singola unità”.

---

## 50.000 misurazioni/anno: è un problema?
In generale: **no**, per un DB relazionale standard non è un volume alto.

Ordine di grandezza:
- 50.000 righe/anno su `cubage_measurement`;
- in 5 anni: ~250.000 righe;
- con indici corretti (`cubage_id`, `item_id`, chiave univoca) resta un carico gestibile.

Attenzioni pratiche:
- usa tipi numerici appropriati (evita campi testuali enormi);
- indicizza le chiavi di join e ricerca più frequenti;
- evita JSON giganteschi per singola riga se non necessari;
- fai manutenzione periodica indici/statistiche secondo il DB engine.

Conclusione: 50k/anno è assolutamente sostenibile; i problemi nascono più da query non indicizzate che dal volume in sé.

---

## Implementazione consigliata in 2 step
1. **Step DB minimo**
   - crea `cubage(id, firing_id)`
   - crea `cubage_measurement(...)` con `unit_index` e vincolo univoco.
2. **Step app di appoggio**
   - draft locale JSON;
   - publish finale su DB in transazione unica.

Questo ti dà un avvio rapido, coerente col tuo vincolo “no status DB”, e prepara già bene il dato etichetta per singola unità.
