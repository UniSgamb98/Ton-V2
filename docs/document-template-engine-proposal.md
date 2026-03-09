# Proposta: motore template per la sezione Documenti

## Obiettivo
Costruire nella sezione **Documenti** uno strumento per creare template riutilizzabili (schede laboratorio, etichette, istruzioni) con **zone dinamiche** valorizzate in base ai parametri di un caso reale (item, lotto, composizione, operatore, data, ecc.).

## Idea chiave: Template + Data Provider
Separare il problema in due parti:

1. **Template Designer**
   - L'utente compone un documento (testo + blocchi dinamici).
   - I blocchi hanno placeholder leggibili, es. `{{item.code}}`, `{{lot.expiry_date}}`, `{{query.total_layers}}`.

2. **Data Provider**
   - Dato un contesto (es. `itemId=123`), raccoglie i dati e restituisce una mappa chiave/valore usata dal render finale.

Questa separazione evita che la logica SQL “invada” il layout del documento.

---

## Struttura consigliata del template
Formato JSON (o YAML) salvato in DB/file:

```json
{
  "templateId": "lab-sheet-v1",
  "name": "Scheda laboratorio standard",
  "engine": "mustache-like",
  "body": "Articolo: {{item.code}}\nComposizione: {{composition.name}}\nStrati: {{query.layer_count}}",
  "dataBindings": [
    {
      "key": "query.layer_count",
      "type": "sql",
      "sql": "SELECT COUNT(*) AS value FROM composition_layer WHERE composition_id = :compositionId"
    }
  ]
}
```

### Convenzione placeholder
- `{{entity.field}}` → campi noti e sicuri (mappati dal backend).
- `{{query.alias}}` → risultati di query custom definite nel template.
- `{{param.name}}` → parametri di input passati in fase di generazione.



## Markup del documento (tabelle, separatori, grassetto)
Per supportare davvero la consegna ai ragazzi in laboratorio conviene introdurre un **markup semplice** (stile Markdown), senza obbligare l'utente a conoscere HTML.

### Sintassi proposta (MVP)
- `**testo**` → grassetto.
- `---` → linea di separazione orizzontale.
- Tabelle con pipe:

```text
| Fase | Materiale | Quantità |
|------|-----------|----------|
| 1    | {{powder.name}} | {{powder.qty}} g |
```

- Blocchi ripetuti per righe dinamiche (lista ingredienti/strati):

```text
{{#each query.layers}}
| {{index}} | {{name}} | {{thickness}} mm |
{{/each}}
```

### Perché è utile
- L'utente scrive veloce in editor testuale.
- Il renderer converte il markup in HTML/PDF stampabile.
- Rimane leggibile anche senza anteprima.

### Regole tecniche consigliate
1. Il body del template è in `doc-markup` (dialetto controllato tipo Markdown).
2. Parsing in AST (blocchi, paragrafi, tabella, hr, inline bold).
3. Rendering in HTML e poi PDF.
4. Validazione preventiva di tag e blocchi `each` (apertura/chiusura).

### Estensione JSON del template
```json
{
  "templateId": "lab-sheet-v2",
  "engine": "doc-markup-v1",
  "body": "**Scheda Lavorazione**\n---\n| Campo | Valore |\n|------|--------|\n| Articolo | {{item.code}} |"
}
```

---

## SQL sì, ma con guardrail
Se volete dare SQL all'utente, conviene farlo in modalità **controllata**:

1. **Solo SELECT** (vietare INSERT/UPDATE/DELETE/DDL).
2. **Named parameters obbligatori** (`:itemId`, `:lotId`) per evitare concatenazioni.
3. **Timeout query** (es. 2-3s) e limite righe (`LIMIT`).
4. **Whitelist tabelle/viste** (meglio ancora: viste dedicate).
5. **Validazione statica** prima del salvataggio template.
6. **Esecuzione con utente DB read-only**.

### Variante migliore (consigliata)
Invece di SQL libero totale:
- offrire **catalogo di viste sicure** (`v_doc_item`, `v_doc_lot`, `v_doc_composition`),
- permettere query solo su quel catalogo.

Riduce errori e rischi senza togliere flessibilità.

---

## Flusso utente proposto
1. Crea template.
2. Scrive testo documento.
3. Inserisce placeholder da picker (non a mano).
4. Aggiunge (opzionale) query SQL per campi avanzati.
5. Testa anteprima con parametri esempio.
6. Salva versione template.
7. In produzione: seleziona template + parametri reali → genera PDF/stampa.

---

## Architettura tecnica (MVP)

### Componenti backend
- `TemplateRepository`
  - salva versione template.
- `TemplateValidator`
  - valida placeholder, SQL, parametri obbligatori.
- `QueryExecutor`
  - esegue binding SQL in sandbox read-only.
- `DocumentRenderService`
  - unisce dati + template e produce output (HTML/PDF).

### Contratto di rendering
Input:
- `templateId`
- `parameters` (`itemId`, `productionId`, `operatorName`...)

Output:
- documento renderizzato
- log di sostituzione placeholder
- eventuali warning (placeholder senza valore)

---

## Esempio pratico
Template body:

```text
Scheda lavorazione
Articolo: {{item.code}}
Lotto: {{lot.code}}
Data consegna: {{param.deliveryDate}}
Numero strati: {{query.layer_count}}
```

Query binding:

```sql
SELECT COUNT(*) AS value
FROM composition_layer cl
JOIN composition c ON c.id = cl.composition_id
JOIN item i ON i.composition_id = c.id
WHERE i.id = :itemId
```

Parametri runtime:

```json
{
  "itemId": 341,
  "deliveryDate": "2026-03-20"
}
```

---

## Piano di implementazione incrementale
1. **Fase 1 (rapida)**
   - Placeholder standard senza SQL custom.
   - Render preview + export PDF.
2. **Fase 2**
   - SQL binding solo su viste whitelisted.
   - Test validazione e permessi.
3. **Fase 3**
   - Versioning template, audit, archivio output.
   - Libreria snippet (header laboratorio, firma, footer qualità).

---

## Decisione pratica per partire subito
Se volete partire domani senza bloccarvi:
- adottate `{{...}}` come sintassi standard,
- create 10-20 placeholder “ufficiali” (item, lot, composizione, date),
- rinviate SQL libero alla fase 2,
- predisponete già il modello `dataBindings` per non rifare tutto dopo.

In questo modo avete valore immediato per il laboratorio e una strada chiara per arrivare alla flessibilità completa.
