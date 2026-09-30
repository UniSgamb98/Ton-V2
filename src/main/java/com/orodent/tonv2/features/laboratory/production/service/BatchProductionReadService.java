package com.orodent.tonv2.features.laboratory.production.service;

import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.core.database.implementation.ItemRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.LineRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.ProductRepositoryImpl;
import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.LineRepository;
import com.orodent.tonv2.core.database.repository.ProductRepository;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.service.TemplatePresetCodes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BatchProductionReadService {

    private final ConnectionProvider connectionProvider;
    private final TemplateEditorService templateEditorService;

    public BatchProductionReadService(ConnectionProvider connectionProvider,
                                      TemplateEditorService templateEditorService) {
        this.connectionProvider = connectionProvider;
        this.templateEditorService = templateEditorService;
    }

    public InitialData loadInitialData() {
        List<Line> lines = connectionProvider.withConnection(connection -> {
            LineRepository repository = new LineRepositoryImpl(connection);
            Map<String, Line> byName = new LinkedHashMap<>();
            for (Line line : repository.findAll()) {
                byName.putIfAbsent(line.name(), line);
            }
            return new ArrayList<>(byName.values());
        });

        List<String> templateNames = templateEditorService.getSavedTemplates().stream()
                .filter(template -> TemplatePresetCodes.PRODUCTION.equals(template.presetCode()))
                .map(TemplateEditorService.TemplateSnapshot::name)
                .toList();

        return new InitialData(
                lines,
                templateNames,
                templateEditorService.getLastBatchTemplateName()
        );
    }

    public List<Product> findProductsByLineName(String lineName) {
        if (lineName == null || lineName.isBlank()) {
            return List.of();
        }

        return connectionProvider.withConnection(connection -> {
            LineRepository lineRepository = new LineRepositoryImpl(connection);
            ProductRepository productRepository = new ProductRepositoryImpl(connection);
            Map<Integer, Product> productsById = new LinkedHashMap<>();

            for (Line line : lineRepository.findAll()) {
                if (!lineName.equals(line.name())) {
                    continue;
                }

                Product product = productRepository.findById(line.productId());
                if (product != null) {
                    productsById.putIfAbsent(product.id(), product);
                }
            }

            return new ArrayList<>(productsById.values());
        });
    }

    public List<Item> findItemsByProduct(int productId) {
        return connectionProvider.withConnection(connection -> {
            ItemRepository repository = new ItemRepositoryImpl(connection);
            return repository.findByProduct(productId);
        });
    }

    public record InitialData(List<Line> lines,
                              List<String> templateNames,
                              String selectedTemplateName) {
    }
}
