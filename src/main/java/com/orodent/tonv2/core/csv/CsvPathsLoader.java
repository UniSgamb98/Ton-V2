package com.orodent.tonv2.core.csv;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;

import java.io.FileReader;
import java.io.IOException;

public class CsvPathsLoader {

    private static final String PATH = "config/csv_paths.json";

    public static CsvPaths load() {
        try (FileReader reader = new FileReader(PATH)) {
            return new Gson().fromJson(reader, CsvPaths.class);
        } catch (IOException | JsonIOException e) {
            throw new RuntimeException("Errore nel caricamento di csv_paths.json", e);
        }
    }
}
