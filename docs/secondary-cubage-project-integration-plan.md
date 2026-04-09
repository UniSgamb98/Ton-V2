# Piano operativo: progetto secondario per integrazione cubaggio -> DB principale

## Contesto
Hai già un progetto secondario che:
- dialoga con i due strumenti di misura;
- esegue il cubaggio operativo;
- **non** fa ancora calcoli etichetta;
- **non** scrive ancora sul DB principale.

Obiettivo ora: trasformarlo nel **bridge ufficiale** tra strumenti e database TON-V2.

---

## Cosa deve fare il progetto secondario (MVP)

### 1) Caricare la worklist dal DB principale
Input minimi da leggere:
- firing aperti (`firing`);
- ordini associati (`production_order_firing`);
- righe item/quantità (`production_order_line`).

Risultato: una lista di lavoro del tipo:
- firing X
- item A -> quantità 30
- item B -> quantità 12

### 2) Acquisire misure per singola unità
Per ogni `item` e per ogni unità `1..quantity`, raccogliere almeno:
- `volume_mm3`
- `density_g_cm3`
- opzionale `mass_g`

### 3) Salvare bozza locale (draft JSON)
Come richiesto, nessuno stato draft su DB.
Il progetto secondario salva localmente un file JSON per:
- riprendere sessioni interrotte;
- gestire disconnessioni DB;
- verificare misure prima della pubblicazione.

### 4) Publish finale sul DB
Quando il lotto è confermato:
1. crea un record `cubage(id, firing_id)`;
2. inserisce tutte le righe `cubage_measurement` (una per unità);
3. transazione unica (`commit` totale o `rollback` totale).

---

## Architettura consigliata (senza stravolgere il progetto esistente)

### Moduli
1. **Adapter strumenti** (già ce l’hai)
   - responsabilità: lettura device e normalizzazione misura.

2. **Session Manager** (nuovo)
   - responsabilità: stato runtime sessione, progressivo `unit_index`, validazioni base.

3. **Draft Store JSON** (nuovo)
   - responsabilità: save/load bozza locale.

4. **DB Publisher** (nuovo)
   - responsabilità: mapping verso `cubage` e `cubage_measurement` + transazione.

5. **Sync/Retry Handler** (nuovo)
   - responsabilità: se publish fallisce, coda retry idempotente.

---

## Contratto dati interno (consigliato)
Usa un payload interno stabile, ad esempio:
- `firingId`
- `itemId`
- `unitIndex`
- `volumeMm3`
- `densityGCm3`
- `massG` (nullable)
- `temperatureSnapshot` (opzionale)
- `measuredAt`
- `deviceId`

Questo ti permette in futuro di aggiungere calcoli senza toccare l’acquisizione device.

---

## Regole importanti per robustezza

### Idempotenza publish
Il publish deve poter essere rilanciato senza duplicare righe.
Base tecnica:
- vincolo DB `UNIQUE(cubage_id, item_id, unit_index)`;
- lato app: controllo preventivo o upsert mirato.

### Atomicità
- un publish deve essere all-or-nothing;
- niente salvataggi parziali se metà unità fallisce.

### Tracciabilità minima
Anche senza audit completo, conserva nel JSON locale:
- timestamp acquisizione;
- identificativo device;
- versione software bridge.

---

## Strategia in 3 rilasci

### Rilascio 1 (2-4 giorni)
- lettura worklist da DB;
- draft JSON locale;
- publish su `cubage` + `cubage_measurement`.

### Rilascio 2
- retry robusto + idempotenza completa;
- controlli qualità dato (range volume/densità).

### Rilascio 3
- modulo calcoli etichetta (se/quando decidi di portarli nel progetto secondario).

---

## Risposta pratica alla tua domanda “come lo facciamo adesso?”
Approccio consigliato immediato:
1. non toccare la parte device che già funziona;
2. aggiungere `Session Manager` + `Draft Store JSON`;
3. implementare un solo comando “Publish su DB” transazionale;
4. usare la struttura DB appena aggiunta (`cubage`, `cubage_measurement`).

Così ottieni valore subito, riduci rischio operativo e non blocchi il team con una riscrittura completa.
