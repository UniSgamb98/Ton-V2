package com.orodent.tonv2.features.laboratory.service;

import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.BlankModelHeightOvermaterial;
import com.orodent.tonv2.core.database.model.BlankModelLayer;
import com.orodent.tonv2.core.database.repository.BlankModelHeightOvermaterialRepository;
import com.orodent.tonv2.core.database.repository.BlankModelLayerRepository;
import com.orodent.tonv2.core.database.repository.BlankModelRepository;
import com.orodent.tonv2.features.laboratory.view.partial.BlankModelLayerDraft;
import com.orodent.tonv2.features.laboratory.view.partial.HeightRangeDraft;

import java.util.ArrayList;
import java.util.List;

public class CreateDiskModelService {

    private final BlankModelRepository blankModelRepo;
    private final BlankModelHeightOvermaterialRepository overmaterialRepo;
    private final BlankModelLayerRepository layerRepo;

    public CreateDiskModelService(BlankModelRepository blankModelRepo,
                                  BlankModelHeightOvermaterialRepository overmaterialRepo,
                                  BlankModelLayerRepository layerRepo) {
        this.blankModelRepo = blankModelRepo;
        this.overmaterialRepo = overmaterialRepo;
        this.layerRepo = layerRepo;
    }

    public SaveResult save(SaveRequest request) {
        try {
            String code = requireText(request.code(), "Codice modello");
            double diameter = parsePositive(request.diameter(), "Diametro");
            double superior = parseNonNegative(request.superiorOvermaterial(), "Overmaterial superiore default");
            double inferior = parseNonNegative(request.inferiorOvermaterial(), "Overmaterial inferiore default");
            double pressure = parsePositive(request.pressure(), "Pressione");
            double gramsPerMm = parsePositive(request.gramsPerMm(), "Grammi per mm");

            List<BlankModelHeightOvermaterial> ranges = buildHeightRanges(request.rangeDrafts());
            List<BlankModelLayer> layers = buildLayers(request.layerDrafts());

            BlankModel model = blankModelRepo.insert(code, diameter, superior, inferior, pressure, gramsPerMm);

            for (BlankModelHeightOvermaterial range : ranges) {
                overmaterialRepo.insert(new BlankModelHeightOvermaterial(
                        0,
                        model.id(),
                        range.minHeightMm(),
                        range.maxHeightMm(),
                        range.superiorOvermaterialMm(),
                        range.inferiorOvermaterialMm()
                ));
            }

            for (BlankModelLayer layer : layers) {
                layerRepo.insert(new BlankModelLayer(0, model.id(), layer.layerNumber(), layer.occupiedSpacePercent()));
            }

            return SaveResult.success("Il nuovo modello è stato registrato correttamente.");
        } catch (ValidationException ex) {
            return SaveResult.error(ex.getTitle(), ex.getMessage());
        } catch (RuntimeException ex) {
            return SaveResult.error("Errore salvataggio modello", "Non è stato possibile salvare il modello disco: " + ex.getMessage());
        }
    }

    private List<BlankModelHeightOvermaterial> buildHeightRanges(List<HeightRangeDraft> drafts) {
        List<BlankModelHeightOvermaterial> ranges = new ArrayList<>();

        for (HeightRangeDraft draft : drafts) {
            if (isEmptyRange(draft)) {
                continue;
            }

            double min = parseNonNegative(draft.minHeight(), "Min altezza fascia");
            double max = parsePositive(draft.maxHeight(), "Max altezza fascia");
            double sup = parseNonNegative(draft.superiorOvermaterial(), "Overmaterial superiore fascia");
            double inf = parseNonNegative(draft.inferiorOvermaterial(), "Overmaterial inferiore fascia");

            if (max <= min) {
                throw new ValidationException("Intervallo altezza non valido", "Ogni fascia deve avere max altezza maggiore della min altezza.");
            }

            ranges.add(new BlankModelHeightOvermaterial(0, 0, min, max, sup, inf));
        }

        return ranges;
    }

    private List<BlankModelLayer> buildLayers(List<BlankModelLayerDraft> drafts) {
        List<BlankModelLayer> layers = new ArrayList<>();
        double total = 0;

        for (BlankModelLayerDraft draft : drafts) {
            if (draft.occupiedSpacePercent() == null || draft.occupiedSpacePercent().isBlank()) {
                continue;
            }

            double occupiedSpace = parsePositive(draft.occupiedSpacePercent(), "Spazio occupato strato " + draft.layerNumber());
            total += occupiedSpace;
            layers.add(new BlankModelLayer(0, 0, draft.layerNumber(), occupiedSpace));
        }

        if (total > 100) {
            throw new ValidationException("Percentuali strati non valide", "La somma dello spazio occupato degli strati non può superare 100%.");
        }

        return layers;
    }

    private boolean isEmptyRange(HeightRangeDraft draft) {
        return isBlank(draft.minHeight())
                && isBlank(draft.maxHeight())
                && isBlank(draft.superiorOvermaterial())
                && isBlank(draft.inferiorOvermaterial());
    }

    private String requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new ValidationException("Campo obbligatorio mancante", fieldName + " è obbligatorio.");
        }
        return value.trim();
    }

    private double parsePositive(String raw, String fieldName) {
        double value = parseDouble(raw, fieldName);
        if (value <= 0) {
            throw new ValidationException("Valore non valido", fieldName + " deve essere maggiore di 0.");
        }
        return value;
    }

    private double parseNonNegative(String raw, String fieldName) {
        double value = parseDouble(raw, fieldName);
        if (value < 0) {
            throw new ValidationException("Valore non valido", fieldName + " non può essere negativo.");
        }
        return value;
    }

    private double parseDouble(String raw, String fieldName) {
        if (isBlank(raw)) {
            throw new ValidationException("Campo obbligatorio mancante", fieldName + " è obbligatorio.");
        }

        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new ValidationException("Formato numerico non valido", fieldName + " deve essere un numero valido.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record SaveRequest(
            String code,
            String diameter,
            String superiorOvermaterial,
            String inferiorOvermaterial,
            String pressure,
            String gramsPerMm,
            List<HeightRangeDraft> rangeDrafts,
            List<BlankModelLayerDraft> layerDrafts
    ) {}

    public record SaveResult(boolean success, String title, String message) {
        public static SaveResult success(String message) {
            return new SaveResult(true, "Modello disco salvato", message);
        }

        public static SaveResult error(String title, String message) {
            return new SaveResult(false, title, message);
        }
    }

    private static class ValidationException extends RuntimeException {
        private final String title;

        private ValidationException(String title, String message) {
            super(message);
            this.title = title;
        }

        private String getTitle() {
            return title;
        }
    }
}
