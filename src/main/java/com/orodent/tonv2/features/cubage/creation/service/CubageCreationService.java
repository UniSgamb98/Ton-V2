package com.orodent.tonv2.features.cubage.creation.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CubageCreationService {

    private final Map<String, List<PayloadOption>> payloadVersionsByCode = buildPayloadVersions();

    public String getIntroMessage() {
        return "Sezione dedicata alla gestione dei calcoli del cubaggio.";
    }

    public List<PayloadOption> getLatestPayloadOptions() {
        List<PayloadOption> latest = new ArrayList<>();
        for (List<PayloadOption> versions : payloadVersionsByCode.values()) {
            versions.stream()
                    .max(Comparator.comparingInt(PayloadOption::version))
                    .ifPresent(latest::add);
        }
        return latest;
    }

    public List<PayloadOption> getAllVersionsForPayload(String payloadCode) {
        return List.copyOf(payloadVersionsByCode.getOrDefault(payloadCode, List.of()));
    }

    public String buildPayloadPreview(PayloadOption option, boolean selectedFromLegacy) {
        if (option == null) {
            return "Nessun payload selezionato.";
        }

        return """
                Payload selezionato:
                - Codice: %s
                - Versione: v%d
                - Origine selezione: %s
                """.formatted(option.payloadCode(), option.version(), selectedFromLegacy ? "Legacy" : "Attiva");
    }

    private Map<String, List<PayloadOption>> buildPayloadVersions() {
        Map<String, List<PayloadOption>> values = new LinkedHashMap<>();
        values.put("PROJECT2_PAYLOAD", List.of(
                new PayloadOption("PROJECT2_PAYLOAD", 1),
                new PayloadOption("PROJECT2_PAYLOAD", 2),
                new PayloadOption("PROJECT2_PAYLOAD", 3)
        ));
        values.put("PROJECT3_PAYLOAD", List.of(
                new PayloadOption("PROJECT3_PAYLOAD", 1),
                new PayloadOption("PROJECT3_PAYLOAD", 2)
        ));
        return values;
    }

    public record PayloadOption(String payloadCode, int version) {
        public String displayName() {
            return "%s v%d".formatted(payloadCode, version);
        }
    }
}
