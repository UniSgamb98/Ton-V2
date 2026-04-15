package com.orodent.tonv2.features.cubage.creation.service;

import com.orodent.tonv2.core.database.model.PayloadContract;
import com.orodent.tonv2.core.database.model.PayloadContractField;
import com.orodent.tonv2.core.database.repository.PayloadContractFieldRepository;
import com.orodent.tonv2.core.database.repository.PayloadContractRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CubageCreationService {

    private final PayloadContractRepository payloadContractRepository;
    private final PayloadContractFieldRepository payloadContractFieldRepository;

    public CubageCreationService(PayloadContractRepository payloadContractRepository,
                                 PayloadContractFieldRepository payloadContractFieldRepository) {
        this.payloadContractRepository = payloadContractRepository;
        this.payloadContractFieldRepository = payloadContractFieldRepository;
    }

    public String getIntroMessage() {
        return "Sezione dedicata alla gestione dei calcoli del cubaggio.";
    }

    public List<PayloadOption> getLatestPayloadOptions() {
        Map<String, List<PayloadContract>> groupedByCode = payloadContractRepository.findAll().stream()
                .collect(Collectors.groupingBy(PayloadContract::contractCode));

        return groupedByCode.values().stream()
                .map(versions -> versions.stream().max(Comparator.comparingInt(PayloadContract::version)).orElse(null))
                .filter(contract -> contract != null)
                .map(contract -> new PayloadOption(
                        contract.id(),
                        contract.contractCode(),
                        contract.version(),
                        contract.description()
                ))
                .sorted(Comparator.comparing(PayloadOption::payloadCode))
                .toList();
    }

    public List<PayloadOption> getAllVersionsForPayload(String payloadCode) {
        return payloadContractRepository.findByContractCode(payloadCode).stream()
                .map(contract -> new PayloadOption(
                        contract.id(),
                        contract.contractCode(),
                        contract.version(),
                        contract.description()
                ))
                .toList();
    }

    public String buildPayloadPreview(PayloadOption option, boolean selectedFromLegacy) {
        if (option == null) {
            return "Nessun payload selezionato.";
        }

        List<PayloadContractField> fields = payloadContractFieldRepository.findByPayloadContractId(option.payloadContractId());

        String fieldsSection;
        if (fields.isEmpty()) {
            fieldsSection = "- Nessun campo configurato";
        } else {
            fieldsSection = fields.stream()
                    .map(field -> "- %s (%s%s)".formatted(
                            field.fieldKey(),
                            field.dataType(),
                            field.unitCode() == null || field.unitCode().isBlank() ? "" : ", " + field.unitCode()
                    ))
                    .collect(Collectors.joining("\n"));
        }

        return """
                Payload selezionato:
                - Codice: %s
                - Versione: v%d
                - Origine selezione: %s
                - Descrizione: %s
                Campi:
                %s
                """.formatted(
                option.payloadCode(),
                option.version(),
                selectedFromLegacy ? "Legacy" : "Attiva",
                option.description() == null || option.description().isBlank() ? "N/D" : option.description(),
                fieldsSection
        );
    }

    public record PayloadOption(int payloadContractId, String payloadCode, int version, String description) {
        public String displayName() {
            return "%s v%d".formatted(payloadCode, version);
        }
    }
}
