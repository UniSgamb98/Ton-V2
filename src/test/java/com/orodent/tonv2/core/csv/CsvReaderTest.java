package com.orodent.tonv2.core.csv;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvReaderTest {
    private final CsvReader reader = new CsvReader();

    @Test
    void readsSemicolonCsvWithBomQuotedSeparatorsAndEscapedQuotes() {
        String csv = "\uFEFFCodice;Descrizione;Quantità\r\n"
                + "A1;\"Testo; con \"\"virgolette\"\"\";12\r\n";

        CsvDocument document = reader.read(new StringReader(csv), CsvReadOptions.semicolonSeparated());

        assertEquals(3, document.headers().size());
        assertEquals(1, document.rows().size());
        assertEquals(2, document.rows().getFirst().lineNumber());
        assertEquals("Testo; con \"virgolette\"", document.rows().getFirst().requiredValue("Descrizione"));
    }

    @Test
    void preservesLineNumbersWhenQuotedFieldsContainNewlines() {
        String csv = "Codice;Descrizione\nA1;\"prima\nseconda\"\nA2;ultima\n";

        CsvDocument document = reader.read(new StringReader(csv), CsvReadOptions.semicolonSeparated());

        assertEquals("prima\nseconda", document.rows().getFirst().requiredValue("Descrizione"));
        assertEquals(4, document.rows().get(1).lineNumber());
    }

    @Test
    void rejectsRowsWithAnUnexpectedColumnCount() {
        String csv = "A;B\n1;2;3\n";

        CsvParseException error = assertThrows(CsvParseException.class,
                () -> reader.read(new StringReader(csv), CsvReadOptions.semicolonSeparated()));

        assertEquals(2, error.lineNumber());
    }
}
