package com.orodent.tonv2.features.registers.home.service;

import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.core.database.implementation.BlankModelLayerRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.CompositionLayerIngredientRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.CompositionRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.FiringRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.ItemRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.LotRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.PowderRepositoryImpl;
import com.orodent.tonv2.core.database.model.BlankModelLayer;
import com.orodent.tonv2.core.database.model.Composition;
import com.orodent.tonv2.core.database.model.CompositionLayerIngredient;
import com.orodent.tonv2.core.database.model.Firing;
import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Lot;
import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.database.repository.BlankModelLayerRepository;
import com.orodent.tonv2.core.database.repository.CompositionLayerIngredientRepository;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.FiringRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.LotRepository;
import com.orodent.tonv2.core.database.repository.PowderRepository;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.registers.home.model.RegisterSearchResult;
import com.orodent.tonv2.features.registers.home.model.RegisterSearchResult.CompositionDetails;
import com.orodent.tonv2.features.registers.home.model.RegisterSearchResult.CompositionLayerDetails;
import com.orodent.tonv2.features.registers.home.model.RegisterSearchResult.DocumentDetails;
import com.orodent.tonv2.features.registers.home.model.RegisterSearchResult.FiringDetails;
import com.orodent.tonv2.features.registers.home.model.RegisterSearchResult.FiringItemDetails;
import com.orodent.tonv2.features.registers.home.model.RegisterSearchResult.IngredientDetails;
import com.orodent.tonv2.features.registers.home.model.RegisterSearchResult.RegisterIdentity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class RegistersSearchService {

    private final ConnectionProvider connectionProvider;
    private final TemplateEditorService templateEditorService;

    public RegistersSearchService(ConnectionProvider connectionProvider,
                                  TemplateEditorService templateEditorService) {
        this.connectionProvider = connectionProvider;
        this.templateEditorService = templateEditorService;
    }

    public RegisterSearchResult search(String itemCodeRaw, String lotCodeRaw) {
        String itemCode = normalize(itemCodeRaw);
        String lotCode = normalize(lotCodeRaw);

        if (itemCode == null || lotCode == null) {
            return RegisterSearchResult.failure(RegisterSearchResult.FailureReason.INCOMPLETE_CRITERIA, null);
        }

        return withRepositories(repositories -> {
            Item item = repositories.itemRepository().findByCode(itemCode);
            if (item == null) {
                return RegisterSearchResult.failure(RegisterSearchResult.FailureReason.ITEM_NOT_FOUND, itemCode);
            }

            Lot lot = repositories.lotRepository().findByCodeAndItem(lotCode, item.id());
            if (lot == null) {
                return RegisterSearchResult.failure(RegisterSearchResult.FailureReason.LOT_NOT_FOUND, lotCode);
            }

            Firing firing = repositories.firingRepository().findById(lot.firingId());
            CompositionDetails composition = buildCompositionDetails(repositories, item);
            FiringDetails firingDetails = buildFiringDetails(repositories, lot, firing);
            List<DocumentDetails> documents = buildDocumentDetails();

            return RegisterSearchResult.success(
                    new RegisterIdentity(item.code(), lot.code(), composition.version(), lot.firingId()),
                    composition,
                    firingDetails,
                    documents
            );
        });
    }

    public List<String> suggestItemCodesByPrefix(String itemCodePrefix, int limit) {
        return withRepositories(repositories -> repositories.itemRepository()
                .findByCodePrefix(itemCodePrefix, limit).stream()
                .map(Item::code)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList());
    }

    public List<String> suggestItemCodesByLotPrefix(String lotCodePrefix, int limit) {
        return withRepositories(repositories -> repositories.itemRepository().findByLotCodePrefix(lotCodePrefix, limit).stream()
                .map(Item::code)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList());
    }

    public List<String> suggestLotCodesByPrefix(String lotCodePrefix, int limit) {
        return withRepositories(repositories -> repositories.lotRepository().findByCodePrefix(lotCodePrefix, limit).stream()
                .map(Lot::code)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList());
    }

    public List<String> suggestLotCodesByItemCode(String itemCode, String lotCodePrefix, int limit) {
        String normalizedItemCode = normalize(itemCode);
        if (normalizedItemCode == null) {
            return List.of();
        }

        return withRepositories(repositories -> {
            Item item = repositories.itemRepository().findByCode(normalizedItemCode);
            if (item == null) {
                return List.of();
            }

            return repositories.lotRepository().findByCodePrefixAndItem(lotCodePrefix, item.id(), limit).stream()
                    .map(Lot::code)
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .toList();
        });
    }

    private CompositionDetails buildCompositionDetails(Repositories repositories, Item item) {
        Optional<Integer> activeCompositionId = repositories.compositionRepository()
                .findActiveCompositionId(item.productId());

        if (activeCompositionId.isEmpty()) {
            return new CompositionDetails(null, item.blankModelId(), item.heightMm(), List.of(),
                    RegisterSearchResult.CompositionStatus.NO_ACTIVE_COMPOSITION);
        }

        int compositionId = activeCompositionId.get();
        Optional<Composition> composition = repositories.compositionRepository().findById(compositionId);

        List<CompositionLayerIngredient> ingredients = repositories.ingredientRepository().findByCompositionId(compositionId);
        Integer blankModelId = repositories.compositionRepository()
                .findBlankModelIdByCompositionId(compositionId).orElse(item.blankModelId());
        List<BlankModelLayer> blankLayers = repositories.blankModelLayerRepository().findByBlankModelId(blankModelId);

        if (blankLayers.isEmpty()) {
            return new CompositionDetails(composition.map(Composition::version).orElse(null), blankModelId,
                    item.heightMm(), List.of(), RegisterSearchResult.CompositionStatus.NO_MODEL_LAYERS);
        }

        List<CompositionLayerDetails> layers = blankLayers.stream().map(layer -> {
            List<CompositionLayerIngredient> layerIngredients = ingredients.stream()
                    .filter(ingredient -> ingredient.layerNumber() == layer.layerNumber())
                    .toList();

            List<IngredientDetails> ingredientDetails = layerIngredients.stream().map(ingredient -> {
                Powder powder = repositories.powderRepository().findById(ingredient.powderId());
                String label = powder == null
                        ? "Polvere #" + ingredient.powderId()
                        : powder.name() != null && !powder.name().isBlank() ? powder.name() : powder.code();
                return new IngredientDetails(label, ingredient.percentage());
            }).toList();
            return new CompositionLayerDetails(layer.layerNumber(), layer.diskPercentage(), ingredientDetails);
        }).toList();

        return new CompositionDetails(composition.map(Composition::version).orElse(null), blankModelId,
                item.heightMm(), layers, RegisterSearchResult.CompositionStatus.AVAILABLE);
    }

    private FiringDetails buildFiringDetails(Repositories repositories, Lot lot, Firing firing) {
        if (firing == null) {
            return new FiringDetails(lot.firingId(), null, null, null, List.of(),
                    RegisterSearchResult.FiringStatus.NOT_FOUND);
        }

        List<ItemRepository.ItemFiringQuantityRow> itemQuantities = repositories.itemRepository()
                .findItemQuantitiesByFiringId(firing.id());
        List<FiringItemDetails> items = itemQuantities.stream()
                .map(row -> new FiringItemDetails(row.itemCode(), row.quantity()))
                .toList();
        return new FiringDetails(firing.id(), firing.firingDate(), firing.furnace(),
                firing.maxTemperature(), items, items.isEmpty()
                        ? RegisterSearchResult.FiringStatus.NO_ITEMS
                        : RegisterSearchResult.FiringStatus.AVAILABLE);
    }

    private List<DocumentDetails> buildDocumentDetails() {
        return templateEditorService.getSavedTemplates().stream()
                .sorted(Comparator.comparing(TemplateEditorService.TemplateSnapshot::savedAt).reversed())
                .limit(5)
                .map(template -> new DocumentDetails(template.name(), template.presetCode(), template.savedAt()))
                .toList();
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private <T> T withRepositories(Function<Repositories, T> work) {
        return connectionProvider.withConnection(connection -> work.apply(new Repositories(
                new ItemRepositoryImpl(connection),
                new LotRepositoryImpl(connection),
                new FiringRepositoryImpl(connection),
                new CompositionRepositoryImpl(connection),
                new CompositionLayerIngredientRepositoryImpl(connection),
                new BlankModelLayerRepositoryImpl(connection),
                new PowderRepositoryImpl(connection)
        )));
    }

    private record Repositories(ItemRepository itemRepository,
                                LotRepository lotRepository,
                                FiringRepository firingRepository,
                                CompositionRepository compositionRepository,
                                CompositionLayerIngredientRepository ingredientRepository,
                                BlankModelLayerRepository blankModelLayerRepository,
                                PowderRepository powderRepository) {
    }

}
