# Adeguamento codice Java al nuovo schema DB (V2)

Questo documento traduce il nuovo schema DB in una checklist operativa per aggiornare il codice Java dell'applicazione.

## 1) Modello dati Java: cosa cambiare

### 1.1 `Product`
- **Prima**: `type`, `color`.
- **Dopo**: `code`, `description`.
- Azione:
  - sostituire il record `Product(int id, String type, String color)` con `Product(int id, String code, String description)`.
  - aggiornare `toString()` per mostrare `code` (eventualmente `code + " - " + description`).

### 1.2 `Item`
- **Prima**: solo `id`, `code`.
- **Dopo**: `product_id`, `blank_model_id`, `height_mm` (niente `code`).
- Azione:
  - sostituire `Item(int id, String code)` con un record che includa almeno:
    - `id`
    - `productId`
    - `blankModelId`
    - `heightMm`

### 1.3 `BlankModel`
- **Prima**: overmaterial direttamente sul modello (`superior_overmaterial_mm`, `inferior_overmaterial_mm`).
- **Dopo**: default (`superior_overmaterial_default_mm`, `inferior_overmaterial_default_mm`) + `num_layers`.
- Azione:
  - aggiornare il record con i nuovi nomi campo e `numLayers`.

### 1.4 `Composition`
- **Prima**: legata a `item_id`, con `is_active`.
- **Dopo**: legata a `product_id`, con `num_layers`; stato attivo gestito da tabella `product_active_composition`.
- Azione:
  - sostituire campi `itemId`, `active` con `productId`, `numLayers`.

### 1.5 `CompositionLayer` / `CompositionLayerIngredient`
- **Prima**: `composition_layer` separata + ingredienti su `layer_id`.
- **Dopo**: niente tabella `composition_layer`; ingredienti direttamente su `(composition_id, layer_number, powder_id)`.
- Azione:
  - eliminare modello/repository legati a `CompositionLayer`.
  - aggiornare `CompositionLayerIngredient` per usare:
    - `compositionId`
    - `layerNumber`
    - `powderId`
    - `percentage`

### 1.6 Produzione
- **Prima**: `production` singola riga per item.
- **Dopo**: `production_order` (testata) + `production_order_line` (righe).
- Azione:
  - rimpiazzare `Production` con:
    - `ProductionOrder`
    - `ProductionOrderLine`

### 1.7 Firing/Lot
- **Dopo**: relazione con produzione via `production_order_firing`; tabella `lot` non presente nel nuovo schema condiviso.
- Azione:
  - verificare se `Lot`, `LotRepository`, `Stock` vanno mantenuti con estensione schema locale o se vanno rimossi/riprogettati.

---

## 2) Repository: refactor richiesto

### 2.1 `ProductRepository`
- query e insert su colonne nuove (`code`, `description`).
- rimuovere dipendenza da `type/color`.

### 2.2 `ItemRepository`
- eliminare metodi basati su `code` (`findByCode`, `insert(String code)`).
- introdurre metodi coerenti con chiave logica:
  - `findByProduct(int productId)`
  - `findByProductAndHeight(int productId, BigDecimal heightMm)`
  - `insert(int productId, int blankModelId, BigDecimal heightMm)`

### 2.3 `BlankModelRepository`
- aggiornare SQL per i campi default overmaterial + `num_layers`.

### 2.4 `CompositionRepository`
- cambiare metodi da item-based a product-based:
  - `findMaxVersionByProduct(productId)`
  - `findLatestByProduct(productId)`
- rimuovere `deactivateActiveByProduct(...)` (non esiste più `is_active`).
- aggiungere gestione tabella `product_active_composition`:
  - `setActiveComposition(productId, compositionId)` (upsert)
  - `findActiveCompositionId(productId)`

### 2.5 Ingredienti composizione
- rimuovere `CompositionLayerRepository`.
- aggiornare `CompositionLayerIngredientRepository`:
  - insert/update/select su `(composition_id, layer_number, powder_id, percentage)`.
  - ricerca per composizione (e ordinamento per layer_number).

### 2.6 Produzione
- sostituire `ProductionRepository` con:
  - `ProductionOrderRepository` (CRUD testata)
  - `ProductionOrderLineRepository` (CRUD righe)
  - opzionale metodo transazionale `createOrderWithLines(...)`.

### 2.7 Firing
- repository per tabella ponte `production_order_firing`.
- aggiornare vincolo logico su `firing`: `UNIQUE(firing_date, furnace)`.

---

## 3) Controller/Service: flussi da riscrivere

### 3.1 Creazione composizione (`CreateCompositionController`)
- oggi il flusso crea/trova un `item` tecnico per il prodotto e salva composizione su item.
- nel nuovo schema:
  1. selezione/creazione `product`.
  2. calcolo versione su `product`.
  3. insert `composition(product_id, version, num_layers, ...)`.
  4. insert ingredienti con `layer_number` diretto.
  5. attivazione tramite `product_active_composition` (solo quando layer completi al 100%).
- `num_layers` deve diventare input esplicito o derivazione controllata dal numero di layer UI.

### 3.2 Caricamento ultima versione
- ora usa `CompositionLayerRepository` + `findByLayerId`.
- nuovo flusso:
  - leggere ingredienti con query unica per `composition_id` ordinando `layer_number`.
  - ricostruire i `LayerDraft` raggruppando per `layer_number`.

### 3.3 Documento di produzione
- nuovo caso d'uso:
  - creazione `production_order` con (`product_id`, `composition_id`, `blank_model_id`, data, note).
  - aggiunta righe per più `item` (altezza/quantità).
- la UI deve passare da “produzione singola item” a “ordine con tabella righe”.

### 3.4 Calcolo overmaterial per item
- algoritmo da introdurre nel service:
  1. trovare override in `blank_model_height_overmaterial` dove `min_height_mm <= height_mm < max_height_mm`.
  2. fallback ai default su `blank_model` se nessun override.

---

## 4) Gestione errori trigger (`SQLSTATE = 45000`)

Aggiungere una gestione centralizzata per eccezioni SQL con messaggi user-friendly.

Suggerimento:
- utility `SqlErrorMapper` che, dato `SQLException`, intercetta:
  - `sqlEx.getSQLState().equals("45000")`
  - mostra `sqlEx.getMessage()` (o mapping custom per trigger noti).
- usare questa utility nei controller quando si salva:
  - overmaterial range,
  - associazione composition↔blank_model,
  - ingredienti per layer,
  - attivazione composizione.

Questo evita `RuntimeException` generiche e rende chiaro all'utente il motivo del blocco DB.

---

## 5) Piano di implementazione consigliato (ordine)

1. **Allineare modello Java + repository SQL** a nuove colonne/tabelle.
2. **Rifattorizzare composizioni** (via `product_active_composition`, rimozione `composition_layer`).
3. **Introdurre produzione testata+righe**.
4. **Aggiornare firing bridge** (`production_order_firing`).
5. **Integrare gestione errori 45000** e validazioni UI preventive.
6. **Aggiornare test/integration test** con casi trigger (overlap range, layer > 100, attivazione incompleta).

---

## 6) Impatti diretti nel codice attuale (file da toccare)

- Model:
  - `core/database/model/Product.java`
  - `core/database/model/Item.java`
  - `core/database/model/BlankModel.java`
  - `core/database/model/Composition.java`
  - `core/database/model/CompositionLayerIngredient.java`
  - rimozione/phase-out `core/database/model/CompositionLayer.java`
  - sostituzione `core/database/model/Production.java` con order + line

- Repository/API:
  - `core/database/repository/ProductRepository.java`
  - `core/database/repository/ItemRepository.java`
  - `core/database/repository/BlankModelRepository.java`
  - `core/database/repository/CompositionRepository.java`
  - `core/database/repository/CompositionLayerRepository.java` (da rimuovere)
  - `core/database/repository/CompositionLayerIngredientRepository.java`
  - `core/database/repository/ProductionRepository.java` (da sostituire)

- Implementazioni SQL:
  - `core/database/implementation/ProductRepositoryImpl.java`
  - `core/database/implementation/ItemRepositoryImpl.java`
  - `core/database/implementation/BlankModelRepositoryImpl.java`
  - `core/database/implementation/CompositionRepositoryImpl.java`
  - `core/database/implementation/CompositionLayerRepositoryImpl.java` (phase-out)
  - `core/database/implementation/CompositionLayerIngredientRepositoryImpl.java`
  - `core/database/implementation/ProductionRepositoryImpl.java`

- Flussi UI/controller:
  - `features/laboratory/controller/CreateCompositionController.java`
  - viste collegate a selezione prodotto/layer/produzione.

