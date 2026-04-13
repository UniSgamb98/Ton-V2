package com.orodent.tonv2.features.registers.home.service;

import com.orodent.tonv2.core.database.model.Firing;
import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Lot;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.FiringRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.LotRepository;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class RegistersSearchService {

    private final ItemRepository itemRepository;
    private final LotRepository lotRepository;
    private final FiringRepository firingRepository;
    private final CompositionRepository compositionRepository;
    private final TemplateEditorService templateEditorService;

    public RegistersSearchService(ItemRepository itemRepository,
                                  LotRepository lotRepository,
                                  FiringRepository firingRepository,
                                  CompositionRepository compositionRepository,
                                  TemplateEditorService templateEditorService) {
        this.itemRepository = itemRepository;
        this.lotRepository = lotRepository;
        this.firingRepository = firingRepository;
        this.compositionRepository = compositionRepository;
        this.templateEditorService = templateEditorService;
    }

    public SearchResult search(String itemCodeRaw, String lotCodeRaw) {
        String itemCode = normalize(itemCodeRaw);
        String lotCode = normalize(lotCodeRaw);

        if (itemCode == null || lotCode == null) {
            String message = "Inserisci sia Articolo che Lotto per avviare la ricerca.";
            return SearchResult.error(message, message, message);
        }

        Item item = itemRepository.findByCode(itemCode);
        if (item == null) {
            String message = "Articolo non trovato: " + itemCode;
            return SearchResult.error(message, message, message);
        }

        Lot lot = lotRepository.findByCodeAndItem(lotCode, item.id());
        if (lot == null) {
            String message = "Lotto non trovato per l'articolo selezionato: " + lotCode;
            return SearchResult.error(message, message, message);
        }

        Firing firing = firingRepository.findById(lot.firingId());

        String compositionSummary = buildCompositionSummary(item);
        String firingSummary = buildFiringSummary(item, lot, firing);
        String documentsSummary = buildDocumentsSummary(item, lot, firing);

        return SearchResult.success(compositionSummary, firingSummary, documentsSummary);
    }

    public List<String> suggestItemCodesByPrefix(String itemCodePrefix, int limit) {
        return itemRepository.findByCodePrefix(itemCodePrefix, limit).stream()
                .map(Item::code)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
    }

    public List<String> suggestItemCodesByLotPrefix(String lotCodePrefix, int limit) {
        return itemRepository.findByLotCodePrefix(lotCodePrefix, limit).stream()
                .map(Item::code)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
    }

    public List<String> suggestLotCodesByPrefix(String lotCodePrefix, int limit) {
        return lotRepository.findByCodePrefix(lotCodePrefix, limit).stream()
                .map(Lot::code)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
    }

    public List<String> suggestLotCodesByItemCode(String itemCode, String lotCodePrefix, int limit) {
        String normalizedItemCode = normalize(itemCode);
        if (normalizedItemCode == null) {
            return List.of();
        }

        Item item = itemRepository.findByCode(normalizedItemCode);
        if (item == null) {
            return List.of();
        }

        return lotRepository.findByCodePrefixAndItem(lotCodePrefix, item.id(), limit).stream()
                .map(Lot::code)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
    }

    private String buildCompositionSummary(Item item) {
        Optional<Integer> activeCompositionId = compositionRepository.findActiveCompositionId(item.productId());

        StringBuilder builder = new StringBuilder();
        builder.append("Articolo: ").append(item.code()).append(System.lineSeparator());
        builder.append("Item ID: ").append(item.id()).append(System.lineSeparator());
        builder.append("Product ID: ").append(item.productId()).append(System.lineSeparator());
        builder.append("Blank Model ID: ").append(item.blankModelId()).append(System.lineSeparator());
        builder.append("Altezza (mm): ").append(item.heightMm()).append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("Composizione attiva: ");
        builder.append(activeCompositionId.map(String::valueOf).orElse("non trovata"));

        return builder.toString();
    }

    private String buildFiringSummary(Item item, Lot lot, Firing firing) {
        StringBuilder builder = new StringBuilder();
        builder.append("Articolo: ").append(item.code()).append(System.lineSeparator());
        builder.append("Lotto: ").append(lot.code()).append(System.lineSeparator());
        builder.append("Firing ID (da lotto): ").append(lot.firingId()).append(System.lineSeparator());

        if (firing == null) {
            builder.append(System.lineSeparator());
            builder.append("Dettagli firing non trovati.");
            return builder.toString();
        }

        builder.append(System.lineSeparator());
        builder.append("Data firing: ").append(firing.firingDate()).append(System.lineSeparator());
        builder.append("Forno: ").append(firing.furnace()).append(System.lineSeparator());
        builder.append("Temperatura max: ").append(firing.maxTemperature()).append(System.lineSeparator());
        builder.append("Note: ").append(firing.notes() == null ? "" : firing.notes());

        return builder.toString();
    }

    private String buildDocumentsSummary(Item item, Lot lot, Firing firing) {
        List<TemplateEditorService.TemplateSnapshot> templates = templateEditorService.getSavedTemplates().stream()
                .sorted(Comparator.comparing(TemplateEditorService.TemplateSnapshot::savedAt).reversed())
                .toList();

        StringBuilder builder = new StringBuilder();
        builder.append("Documento pronto per ricostruzione dati").append(System.lineSeparator());
        builder.append("Articolo: ").append(item.code()).append(System.lineSeparator());
        builder.append("Lotto: ").append(lot.code()).append(System.lineSeparator());
        builder.append("Firing ID: ").append(firing == null ? "n/d" : firing.id()).append(System.lineSeparator());
        builder.append(System.lineSeparator());

        if (templates.isEmpty()) {
            builder.append("Nessun template salvato disponibile.");
            return builder.toString();
        }

        builder.append("Template disponibili (ultimi 5):").append(System.lineSeparator());
        templates.stream()
                .limit(5)
                .forEach(template -> builder
                        .append("- ")
                        .append(template.name())
                        .append(" [preset=")
                        .append(template.presetCode() == null ? "" : template.presetCode())
                        .append("]")
                        .append(System.lineSeparator()));

        return builder.toString();
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    public record SearchResult(boolean success,
                               String compositionOutput,
                               String firingOutput,
                               String documentsOutput) {

        static SearchResult success(String compositionOutput, String firingOutput, String documentsOutput) {
            return new SearchResult(true, compositionOutput, firingOutput, documentsOutput);
        }

        static SearchResult error(String compositionOutput, String firingOutput, String documentsOutput) {
            return new SearchResult(false, compositionOutput, firingOutput, documentsOutput);
        }
    }
}
