package com.orodent.tonv2.features.laboratory.production.service;

import com.orodent.tonv2.core.database.model.Line;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Builds document params for batch production templates.
 */
public class BatchProductionDocumentParamsService {

    private final Supplier<Connection> connectionSupplier;

    public BatchProductionDocumentParamsService(Supplier<Connection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    public Map<String, Object> buildParams(Line line,
                                           String notes,
                                           List<BatchProductionService.ProductionPlanLine> planLines) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("line", Map.of("name", line == null || line.name() == null ? "" : line.name()));
        params.put("notes", notes == null ? "" : notes);
        params.put("items", buildItems(planLines));

        if (planLines == null || planLines.isEmpty()) {
            params.put("composition", Map.of());
            params.put("blank_model", Map.of());
            return params;
        }

        int compositionId = planLines.getFirst().compositionId();
        int blankModelId = planLines.getFirst().item().blankModelId();

        params.put("composition", fetchComposition(compositionId));
        params.put("blank_model", fetchBlankModel(blankModelId));
        return params;
    }

    public Map<String, Object> buildSamplePresetFromDb() {
        try (Connection connection = connectionSupplier.get()) {
            SampleRow sample = findSampleRow(connection);
            if (sample == null) {
                return Map.of(
                        "line", Map.of("name", ""),
                        "notes", "",
                        "items", List.of(),
                        "composition", Map.of(),
                        "blank_model", Map.of()
                );
            }

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("line", Map.of("name", sample.lineName));
            params.put("notes", "Preset automatico da DB");
            params.put("items", List.of(Map.of(
                    "code", sample.itemCode,
                    "quantity", 1,
                    "height_mm", sample.heightMm
            )));
            params.put("composition", fetchComposition(sample.compositionId));
            params.put("blank_model", fetchBlankModel(sample.blankModelId));
            return params;
        } catch (SQLException e) {
            throw new RuntimeException("Errore costruzione preset da DB", e);
        }
    }

    private SampleRow findSampleRow(Connection connection) throws SQLException {
        String sql = """
                SELECT i.id AS item_id,
                       i.code AS item_code,
                       i.height_mm,
                       i.blank_model_id,
                       pac.composition_id,
                       COALESCE(l.name, 'Linea') AS line_name
                FROM item i
                JOIN product_active_composition pac ON pac.product_id = i.product_id
                LEFT JOIN line l ON l.product_id = i.product_id
                FETCH FIRST 1 ROWS ONLY
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }

            return new SampleRow(
                    rs.getInt("item_id"),
                    rs.getString("item_code"),
                    rs.getDouble("height_mm"),
                    rs.getInt("blank_model_id"),
                    rs.getInt("composition_id"),
                    rs.getString("line_name")
            );
        }
    }

    private Map<String, Object> fetchComposition(int compositionId) {
        try (Connection connection = connectionSupplier.get()) {
            Map<String, Object> composition = new LinkedHashMap<>();
            String compositionSql = "SELECT version, num_layers FROM composition WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(compositionSql)) {
                ps.setInt(1, compositionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        composition.put("version", rs.getInt("version"));
                        composition.put("num_layers", rs.getInt("num_layers"));
                    }
                }
            }

            composition.put("layers", fetchCompositionLayers(connection, compositionId));
            return composition;
        } catch (SQLException e) {
            throw new RuntimeException("Errore lettura composition", e);
        }
    }

    private List<Map<String, Object>> fetchCompositionLayers(Connection connection, int compositionId) throws SQLException {
        String sql = """
                SELECT cli.layer_number,
                       cli.percentage,
                       cli.powder_id,
                       COALESCE(p.code, '') AS powder_code
                FROM composition_layer_ingredient cli
                LEFT JOIN powder p ON p.id = cli.powder_id
                WHERE cli.composition_id = ?
                ORDER BY cli.layer_number, cli.powder_id
                """;

        Map<Integer, List<Map<String, Object>>> groupedIngredients = new LinkedHashMap<>();
        Map<Integer, Double> groupedPercentages = new LinkedHashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, compositionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int layer = rs.getInt("layer_number");
                    groupedIngredients.computeIfAbsent(layer, ignored -> new ArrayList<>())
                            .add(Map.of(
                                    "powder", Map.of(
                                            "id", rs.getInt("powder_id"),
                                            "code", rs.getString("powder_code")
                                    )
                            ));
                    groupedPercentages.merge(layer, rs.getDouble("percentage"), Double::sum);
                }
            }
        }

        List<Map<String, Object>> layers = new ArrayList<>();
        for (Map.Entry<Integer, List<Map<String, Object>>> entry : groupedIngredients.entrySet()) {
            layers.add(Map.of(
                    "layer_number", entry.getKey(),
                    "percentage", groupedPercentages.getOrDefault(entry.getKey(), 0.0),
                    "ingredients", entry.getValue()
            ));
        }
        return layers;
    }

    private Map<String, Object> fetchBlankModel(int blankModelId) {
        try (Connection connection = connectionSupplier.get()) {
            Map<String, Object> result = new LinkedHashMap<>();
            String blankSql = """
                    SELECT code,
                           pressure_kg_cm2,
                           grams_per_mm,
                           diameter_mm,
                           superior_overmaterial_default_mm,
                           inferior_overmaterial_default_mm
                    FROM blank_model
                    WHERE id = ?
                    """;
            try (PreparedStatement ps = connection.prepareStatement(blankSql)) {
                ps.setInt(1, blankModelId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        result.put("code", rs.getString("code"));
                        result.put("pressure_kg_cm2", rs.getDouble("pressure_kg_cm2"));
                        result.put("grams_per_mm", rs.getDouble("grams_per_mm"));
                        result.put("diameter_mm", rs.getDouble("diameter_mm"));
                        result.put("superior_overmaterial_default_mm", rs.getDouble("superior_overmaterial_default_mm"));
                        result.put("inferior_overmaterial_default_mm", rs.getDouble("inferior_overmaterial_default_mm"));
                    }
                }
            }

            result.put("layers", fetchBlankModelLayers(connection, blankModelId));
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Errore lettura blank model", e);
        }
    }

    private List<Map<String, Object>> fetchBlankModelLayers(Connection connection, int blankModelId) throws SQLException {
        String sql = """
                SELECT layer_number, disk_percentage
                FROM blank_model_layer
                WHERE blank_model_id = ?
                ORDER BY layer_number
                """;

        List<Map<String, Object>> layers = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, blankModelId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    layers.add(Map.of(
                            "layer_number", rs.getInt("layer_number"),
                            "disk_percentage", rs.getDouble("disk_percentage")
                    ));
                }
            }
        }
        return layers;
    }

    private List<Map<String, Object>> buildItems(List<BatchProductionService.ProductionPlanLine> planLines) {
        if (planLines == null) {
            return List.of();
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (BatchProductionService.ProductionPlanLine line : planLines) {
            items.add(Map.of(
                    "code", line.item().code(),
                    "quantity", line.quantity(),
                    "height_mm", line.item().heightMm()
            ));
        }
        return items;
    }

    private record SampleRow(
            int itemId,
            String itemCode,
            double heightMm,
            int blankModelId,
            int compositionId,
            String lineName
    ) {}
}
