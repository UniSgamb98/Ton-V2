package com.orodent.tonv2.features.laboratory.diskmodel.presentation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiskModelEditorStateTest {

    @Test
    void distributesAndSummarizesLayerPercentages() {
        DiskModelEditorState state = new DiskModelEditorState();

        List<Double> distributed = state.distributeLayers(4);
        DiskModelEditorState.LayerSummary summary = state.summarizeLayers(
                distributed.stream().map(String::valueOf).toList()
        );

        assertEquals(List.of(25.0, 25.0, 25.0, 25.0), distributed);
        assertTrue(summary.valid());
        assertEquals(100.0, summary.total());
    }
}
