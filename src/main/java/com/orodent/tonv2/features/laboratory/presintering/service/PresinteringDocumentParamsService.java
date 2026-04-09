package com.orodent.tonv2.features.laboratory.presintering.service;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.repository.ItemRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PresinteringDocumentParamsService {

    private final ItemRepository itemRepo;

    public PresinteringDocumentParamsService(ItemRepository itemRepo) {
        this.itemRepo = itemRepo;
    }

    public Map<String, Object> buildParams(ParamsRequest request) {
        if (request == null) {
            return Map.of();
        }

        Map<Integer, Integer> plannedItems = request.plannedItemsByItemId() == null
                ? Map.of()
                : request.plannedItemsByItemId();

        List<Map<String, Object>> items = new ArrayList<>();
        int totalQuantity = 0;

        for (Map.Entry<Integer, Integer> entry : plannedItems.entrySet()) {
            int itemId = entry.getKey();
            int quantity = entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
            if (quantity <= 0) {
                continue;
            }

            Item item = itemRepo.findById(itemId);
            String itemCode = item == null || item.code() == null ? "Item " + itemId : item.code();

            items.add(Map.of(
                    "item_id", itemId,
                    "item_code", itemCode,
                    "quantity", quantity
            ));
            totalQuantity += quantity;
        }

        Map<String, Object> furnaceSummary = new LinkedHashMap<>();
        furnaceSummary.put("name", request.furnaceName() == null ? "" : request.furnaceName());
        furnaceSummary.put("temperature", request.maxTemperature());
        furnaceSummary.put("total_quantity", totalQuantity);

        Map<String, Object> firing = new LinkedHashMap<>();
        firing.put("id", request.firingId());
        firing.put("firing_date", request.firingDate() == null ? null : request.firingDate().toString());
        firing.put("furnace", request.furnaceName() == null ? "" : request.furnaceName());
        firing.put("max_temperature", request.maxTemperature());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("firing", firing);
        payload.put("furnace_summary", furnaceSummary);
        payload.put("items", items);
        payload.put("generated_at", Instant.now().toString());
        return payload;
    }

    public Map<String, Object> buildBatchParams(List<FurnaceBatchRequest> furnaces) {
        if (furnaces == null || furnaces.isEmpty()) {
            return Map.of("furnaces", List.of(), "generated_at", Instant.now().toString());
        }

        List<Map<String, Object>> furnacePayloads = new ArrayList<>();
        for (FurnaceBatchRequest furnace : furnaces) {
            Map<Integer, Integer> plannedItems = furnace.plannedItemsByItemId() == null
                    ? Map.of()
                    : furnace.plannedItemsByItemId();

            List<Map<String, Object>> items = new ArrayList<>();
            int totalQuantity = 0;
            for (Map.Entry<Integer, Integer> entry : plannedItems.entrySet()) {
                int itemId = entry.getKey();
                int quantity = entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
                if (quantity <= 0) {
                    continue;
                }

                Item item = itemRepo.findById(itemId);
                String itemCode = item == null || item.code() == null ? "Item " + itemId : item.code();
                items.add(Map.of(
                        "item_id", itemId,
                        "item_code", itemCode,
                        "quantity", quantity
                ));
                totalQuantity += quantity;
            }

            Map<String, Object> furnacePayload = new LinkedHashMap<>();
            furnacePayload.put("name", furnace.furnaceName() == null ? "" : furnace.furnaceName());
            furnacePayload.put("temperature", furnace.maxTemperature());
            furnacePayload.put("firing_id", furnace.firingId());
            furnacePayload.put("firing_date", furnace.firingDate() == null ? null : furnace.firingDate().toString());
            furnacePayload.put("total_quantity", totalQuantity);
            furnacePayload.put("items", items);
            furnacePayloads.add(furnacePayload);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("furnaces", furnacePayloads);
        payload.put("generated_at", Instant.now().toString());
        return payload;
    }

    public Map<String, Object> buildPresetParams(String notes) {
        Item firstItem = itemRepo.findAll().stream().findFirst().orElse(null);
        int itemId = firstItem == null ? 0 : firstItem.id();
        String itemCode = firstItem == null ? "ITEM-DEMO" : firstItem.code();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("furnaces", List.of(Map.of(
                "name", "Forno Demo",
                "temperature", 1530,
                "firing_id", 0,
                "firing_date", LocalDate.now().toString(),
                "total_quantity", 1,
                "items", List.of(Map.of(
                        "item_id", itemId,
                        "item_code", itemCode == null ? "ITEM-DEMO" : itemCode,
                        "quantity", 1
                ))
        )));
        payload.put("notes", notes == null ? "Preset presinterizzazione" : notes);
        payload.put("generated_at", Instant.now().toString());
        return payload;
    }

    public record ParamsRequest(int firingId,
                                LocalDate firingDate,
                                String furnaceName,
                                Integer maxTemperature,
                                Map<Integer, Integer> plannedItemsByItemId) {
    }

    public record FurnaceBatchRequest(int firingId,
                                      LocalDate firingDate,
                                      String furnaceName,
                                      Integer maxTemperature,
                                      Map<Integer, Integer> plannedItemsByItemId) {
    }
}
