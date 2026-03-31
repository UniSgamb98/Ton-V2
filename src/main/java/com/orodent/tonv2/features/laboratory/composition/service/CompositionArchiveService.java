package com.orodent.tonv2.features.laboratory.composition.service;

import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ProductRepository;

import java.util.List;

public class CompositionArchiveService {

    private final ProductRepository productRepository;
    private final CompositionRepository compositionRepository;

    public CompositionArchiveService(ProductRepository productRepository,
                                     CompositionRepository compositionRepository) {
        this.productRepository = productRepository;
        this.compositionRepository = compositionRepository;
    }

    public List<Product> searchProductsWithCompositions(String codeFilter) {
        String normalized = codeFilter == null ? "" : codeFilter.trim().toLowerCase();

        return productRepository.findAll().stream()
                .filter(product -> normalized.isBlank() || product.code().toLowerCase().contains(normalized))
                .filter(product -> compositionRepository.findLatestByProduct(product.id()).isPresent())
                .toList();
    }
}
