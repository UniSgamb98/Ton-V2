package com.orodent.tonv2.features.registers.home.service;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Lot;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.LineRepository;
import com.orodent.tonv2.core.database.repository.LotRepository;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.service.TemplatePresetCodes;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionDocumentParamsService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RegistersDocumentService {

    private final Connection connection;
    private final ItemRepository itemRepository;
    private final LotRepository lotRepository;
    private final LineRepository lineRepository;
    private final TemplateEditorService templateEditorService;
    private final BatchProductionDocumentParamsService batchDocumentParamsService;

    public RegistersDocumentService(Connection connection,
                                    ItemRepository itemRepository,
                                    LotRepository lotRepository,
                                    LineRepository lineRepository,
                                    TemplateEditorService templateEditorService,
                                    BatchProductionDocumentParamsService batchDocumentParamsService) {
        this.connection = connection;
        this.itemRepository = itemRepository;
        this.lotRepository = lotRepository;
        this.lineRepository = lineRepository;
        this.templateEditorService = templateEditorService;
        this.batchDocumentParamsService = batchDocumentParamsService;
    }

    public String generateCompositionDocument(String itemCodeRaw, String lotCodeRaw) {
        String itemCode = normalize(itemCodeRaw, "Inserisci un codice articolo valido.");
        String lotCode = normalize(lotCodeRaw, "Inserisci un codice lotto valido.");

        Item selectedItem = itemRepository.findByCode(itemCode);
        if (selectedItem == null) {
            throw new IllegalArgumentException("Articolo non trovato: " + itemCode);
        }

        Lot lot = lotRepository.findByCodeAndItem(lotCode, selectedItem.id());
        if (lot == null) {
            throw new IllegalArgumentException("Lotto non trovato per l'articolo selezionato: " + lotCode);
        }

        ProductionOrderSnapshot orderSnapshot = findProductionOrderForItemAndFiring(selectedItem.id(), lot.firingId());
        List<BatchProductionService.ProductionPlanLine> planLines = findProductionPlanLines(orderSnapshot.productionOrderId());

        if (planLines.isEmpty()) {
            throw new IllegalArgumentException("Nessuna riga di produzione trovata per l'ordine #" + orderSnapshot.productionOrderId() + ".");
        }

        Line line = lineRepository.findByProductId(orderSnapshot.productId()).stream()
                .findFirst()
                .orElse(new Line(0, "", orderSnapshot.productId()));

        Map<String, Object> params = batchDocumentParamsService.buildParams(
                BatchProductionDocumentParamsService.ParamsRequest.real(
                        line,
                        orderSnapshot.notes(),
                        planLines
                )
        );

        String templateName = resolveCompositionTemplateName();
        String templateText = templateEditorService.getTemplateContentByName(templateName);
        if (templateText == null || templateText.isBlank()) {
            throw new IllegalArgumentException("Template composizione non trovato: " + templateName);
        }

        String payloadJson = templateEditorService.toJson(params);
        TemplateEditorService.PreviewResult renderResult = templateEditorService.previewTemplate(templateText, payloadJson);
        if (!renderResult.success()) {
            throw new IllegalArgumentException("Errore generazione documento: " + renderResult.htmlOrError());
        }

        try {
            Path outputFile = Files.createTempFile("ton-register-composition-", ".html");
            Files.writeString(outputFile, renderResult.htmlOrError());
            return outputFile.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Documento generato ma non salvabile su file temporaneo.");
        }
    }

    private String resolveCompositionTemplateName() {
        String lastTemplateName = templateEditorService.getLastBatchTemplateName();
        if (lastTemplateName != null && !lastTemplateName.isBlank()) {
            return lastTemplateName;
        }

        return templateEditorService.getSavedTemplates().stream()
                .filter(template -> TemplatePresetCodes.PRODUCTION.equals(template.presetCode()))
                .max(Comparator.comparing(TemplateEditorService.TemplateSnapshot::savedAt))
                .map(TemplateEditorService.TemplateSnapshot::name)
                .orElseThrow(() -> new IllegalArgumentException("Nessun template composizione disponibile (preset '" + TemplatePresetCodes.PRODUCTION + "')."));
    }

    private ProductionOrderSnapshot findProductionOrderForItemAndFiring(int itemId, int firingId) {
        String sql = """
                SELECT po.id, po.product_id, po.notes
                FROM production_order po
                JOIN production_order_line_firing polf ON polf.production_order_id = po.id
                WHERE polf.item_id = ?
                  AND polf.firing_id = ?
                ORDER BY po.id DESC
                FETCH FIRST 1 ROW ONLY
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, firingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ProductionOrderSnapshot(
                            rs.getInt("id"),
                            rs.getInt("product_id"),
                            rs.getString("notes")
                    );
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("Errore recupero production_order da item+lot.");
        }

        throw new IllegalArgumentException("Nessun production_order trovato per item/lot selezionati.");
    }

    private List<BatchProductionService.ProductionPlanLine> findProductionPlanLines(int productionOrderId) {
        String sql = """
                SELECT pol.item_id, pol.quantity, po.composition_id
                FROM production_order_line pol
                JOIN production_order po ON po.id = pol.production_order_id
                WHERE pol.production_order_id = ?
                ORDER BY pol.item_id ASC
                """;

        List<BatchProductionService.ProductionPlanLine> lines = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productionOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Item item = itemRepository.findById(rs.getInt("item_id"));
                    if (item == null) {
                        continue;
                    }
                    lines.add(new BatchProductionService.ProductionPlanLine(
                            item,
                            rs.getInt("quantity"),
                            rs.getInt("composition_id")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("Errore recupero righe production_order #" + productionOrderId + ".");
        }

        return lines;
    }

    private String normalize(String value, String errorMessage) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return normalized;
    }

    private record ProductionOrderSnapshot(int productionOrderId, int productId, String notes) {
    }
}
