package com.orodent.tonv2.features.cubage.creation.service;

import com.orodent.tonv2.core.database.model.PayloadContract;
import com.orodent.tonv2.core.database.model.PayloadContractField;
import com.orodent.tonv2.core.database.repository.PayloadContractFieldRepository;
import com.orodent.tonv2.core.database.repository.PayloadContractRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CubageCreationService {

    private static final String INPUT_ROLE = "INPUT";
    private static final String OUTPUT_ROLE = "OUTPUT";

    private final PayloadContractRepository payloadContractRepository;
    private final PayloadContractFieldRepository payloadContractFieldRepository;

    public CubageCreationService(PayloadContractRepository payloadContractRepository,
                                 PayloadContractFieldRepository payloadContractFieldRepository) {
        this.payloadContractRepository = payloadContractRepository;
        this.payloadContractFieldRepository = payloadContractFieldRepository;
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
                        contract.version()
                ))
                .sorted(Comparator.comparing(PayloadOption::payloadCode))
                .toList();
    }

    public List<PayloadOption> getAllVersionsForPayload(String payloadCode) {
        return payloadContractRepository.findByContractCode(payloadCode).stream()
                .map(contract -> new PayloadOption(
                        contract.id(),
                        contract.contractCode(),
                        contract.version()
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

        List<String> requestedOutputs = extractOutputFieldKeys(fields);

        String requestedOutputsSection = requestedOutputs.isEmpty()
                ? "- Nessun output richiesto"
                : requestedOutputs.stream().map(value -> "- " + value).collect(Collectors.joining("\n"));

        return """
                Payload selezionato:
                - Codice: %s
                - Versione: v%d
                - Origine selezione: %s
                Campi:
                %s

                Output richiesto:
                %s
                """.formatted(
                option.payloadCode(),
                option.version(),
                selectedFromLegacy ? "Legacy" : "Attiva",
                fieldsSection,
                requestedOutputsSection
        );
    }

    public FormulaValidationResult validateAndBuildFormulaSet(String formulaSetName,
                                                              String formulasText,
                                                              PayloadOption selectedPayload) {
        if (selectedPayload == null) {
            return FormulaValidationResult.error("Seleziona un payload prima di validare il set formule.");
        }
        if (formulaSetName == null || formulaSetName.isBlank()) {
            return FormulaValidationResult.error("Inserisci il nome del set di calcolo.");
        }
        if (formulasText == null || formulasText.isBlank()) {
            return FormulaValidationResult.error("Inserisci almeno una formula.");
        }

        List<PayloadContractField> fields = payloadContractFieldRepository.findByPayloadContractId(selectedPayload.payloadContractId());
        Set<String> payloadInputFieldKeys = extractInputFieldKeys(fields);
        List<String> requestedFieldKeys = extractOutputFieldKeys(fields);

        List<FormulaRow> formulas = parseAndValidateFormulas(formulasText, payloadInputFieldKeys);
        Set<String> definedVariables = formulas.stream()
                .map(FormulaRow::variable)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> selectedOutputs;
        List<String> missingRequested;
        if (requestedFieldKeys.isEmpty()) {
            selectedOutputs = new ArrayList<>(definedVariables);
            missingRequested = List.of();
        } else {
            selectedOutputs = requestedFieldKeys.stream()
                    .filter(definedVariables::contains)
                    .toList();
            missingRequested = requestedFieldKeys.stream()
                    .filter(key -> !definedVariables.contains(key))
                    .toList();
        }

        String summary = buildValidationSummary(formulaSetName, selectedPayload, formulas.size(), selectedOutputs, missingRequested);
        return FormulaValidationResult.success(summary);
    }

    private List<FormulaRow> parseAndValidateFormulas(String formulasText, Set<String> payloadInputFieldKeys) {
        String[] lines = formulasText.split("\\r?\\n");
        List<FormulaRow> parsed = new ArrayList<>();
        Set<String> definedVariables = new LinkedHashSet<>();

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                continue;
            }

            int eqIndex = line.indexOf('=');
            if (eqIndex <= 0 || eqIndex == line.length() - 1) {
                throw new IllegalArgumentException("Riga " + (i + 1) + ": formato non valido. Usa variabile = espressione.");
            }

            String variable = line.substring(0, eqIndex).trim();
            String expression = line.substring(eqIndex + 1).trim();

            if (!variable.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("Riga " + (i + 1) + ": nome variabile non valido -> " + variable);
            }
            if (definedVariables.contains(variable)) {
                throw new IllegalArgumentException("Riga " + (i + 1) + ": variabile già definita -> " + variable);
            }
            if (payloadInputFieldKeys.contains(variable)) {
                throw new IllegalArgumentException("Riga " + (i + 1) + ": il nome variabile coincide con un campo input del payload -> " + variable);
            }

            Set<String> refs = extractIdentifiers(expression);
            for (String ref : refs) {
                if (!definedVariables.contains(ref) && !payloadInputFieldKeys.contains(ref)) {
                    throw new IllegalArgumentException("Riga " + (i + 1) + ": riferimento non disponibile -> " + ref);
                }
            }

            validateExpressionSyntax(expression, i + 1);
            parsed.add(new FormulaRow(variable, expression));
            definedVariables.add(variable);
        }

        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("Nessuna formula valida trovata nel testo inserito.");
        }

        return parsed;
    }

    private Set<String> extractIdentifiers(String expression) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
                .matcher(expression);

        Set<String> identifiers = new LinkedHashSet<>();
        while (matcher.find()) {
            identifiers.add(matcher.group());
        }
        return identifiers;
    }

    private void validateExpressionSyntax(String expression, int lineNumber) {
        List<String> tokens = tokenizeExpression(expression, lineNumber);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Riga " + lineNumber + ": espressione vuota.");
        }

        Deque<String> stack = new ArrayDeque<>();
        boolean expectOperand = true;

        for (String token : tokens) {
            if ("(".equals(token)) {
                stack.push(token);
                continue;
            }
            if (")".equals(token)) {
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Riga " + lineNumber + ": parentesi chiusa senza apertura.");
                }
                stack.pop();
                expectOperand = false;
                continue;
            }
            if (isOperator(token)) {
                if (expectOperand) {
                    throw new IllegalArgumentException("Riga " + lineNumber + ": operatore in posizione non valida.");
                }
                expectOperand = true;
                continue;
            }
            expectOperand = false;
        }

        if (!stack.isEmpty()) {
            throw new IllegalArgumentException("Riga " + lineNumber + ": parentesi non bilanciate.");
        }
        if (expectOperand) {
            throw new IllegalArgumentException("Riga " + lineNumber + ": espressione incompleta.");
        }
    }

    private List<String> tokenizeExpression(String expression, int lineNumber) {
        List<String> tokens = new ArrayList<>();
        int i = 0;

        while (i < expression.length()) {
            char c = expression.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if ("+-*/()".indexOf(c) >= 0) {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }
            if (Character.isDigit(c) || c == '.') {
                int start = i;
                i++;
                while (i < expression.length()) {
                    char next = expression.charAt(i);
                    if (!Character.isDigit(next) && next != '.') {
                        break;
                    }
                    i++;
                }
                tokens.add(expression.substring(start, i));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                i++;
                while (i < expression.length()) {
                    char next = expression.charAt(i);
                    if (!Character.isLetterOrDigit(next) && next != '_') {
                        break;
                    }
                    i++;
                }
                tokens.add(expression.substring(start, i));
                continue;
            }

            throw new IllegalArgumentException("Riga " + lineNumber + ": carattere non supportato -> " + c);
        }
        return tokens;
    }

    private boolean isOperator(String token) {
        return "+".equals(token) || "-".equals(token) || "*".equals(token) || "/".equals(token);
    }

    private Set<String> extractInputFieldKeys(List<PayloadContractField> fields) {
        return fields.stream()
                .filter(field -> INPUT_ROLE.equalsIgnoreCase(field.fieldRole()))
                .map(PayloadContractField::fieldKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> extractOutputFieldKeys(List<PayloadContractField> fields) {
        return fields.stream()
                .filter(field -> OUTPUT_ROLE.equalsIgnoreCase(field.fieldRole()))
                .sorted(Comparator.comparingInt(PayloadContractField::orderIndex))
                .map(PayloadContractField::fieldKey)
                .toList();
    }

    private String buildValidationSummary(String formulaSetName,
                                          PayloadOption selectedPayload,
                                          int formulasCount,
                                          List<String> selectedOutputs,
                                          List<String> missingRequested) {
        String outputs = selectedOutputs.isEmpty()
                ? "- Nessun output selezionato"
                : selectedOutputs.stream().map(v -> "- " + v).collect(Collectors.joining("\n"));

        String missing = missingRequested.isEmpty()
                ? "- Nessun campo richiesto mancante"
                : missingRequested.stream().map(v -> "- " + v).collect(Collectors.joining("\n"));

        return """
                Set di calcolo valido.
                - Nome: %s
                - Payload: %s v%d
                - Formule valide: %d

                Output selezionati:
                %s

                Campi richiesti mancanti:
                %s
                """.formatted(
                formulaSetName.trim(),
                selectedPayload.payloadCode(),
                selectedPayload.version(),
                formulasCount,
                outputs,
                missing
        );
    }

    public record PayloadOption(int payloadContractId, String payloadCode, int version) {
        public String displayName() {
            return "%s v%d".formatted(payloadCode, version);
        }
    }

    private record FormulaRow(String variable, String expression) {
    }

    public record FormulaValidationResult(boolean valid, String message) {
        public static FormulaValidationResult success(String message) {
            return new FormulaValidationResult(true, message);
        }

        public static FormulaValidationResult error(String message) {
            return new FormulaValidationResult(false, message);
        }
    }
}
