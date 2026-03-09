package com.orodent.tonv2.features.documents.template.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DocumentsTemplateBuilderView extends VBox {
    private final AppHeader header;
    private final TextArea templateArea;
    private final TextArea parametersArea;
    private final TextArea resolvedMarkupArea;
    private final TextArea htmlPreviewArea;
    private final TextArea warningsArea;
    private final Button renderButton;

    public DocumentsTemplateBuilderView() {
        header = new AppHeader("Documenti - Builder Template");
        setSpacing(12);
        setPadding(new Insets(20));

        templateArea = new TextArea();
        templateArea.setPromptText("Inserisci template (doc-markup-v1)");
        templateArea.setPrefRowCount(14);

        parametersArea = new TextArea();
        parametersArea.setPromptText("Inserisci parametri JSON");
        parametersArea.setPrefRowCount(14);

        resolvedMarkupArea = new TextArea();
        resolvedMarkupArea.setEditable(false);
        resolvedMarkupArea.setPromptText("Markup risolto");
        resolvedMarkupArea.setPrefRowCount(10);

        htmlPreviewArea = new TextArea();
        htmlPreviewArea.setEditable(false);
        htmlPreviewArea.setPromptText("HTML generato");
        htmlPreviewArea.setPrefRowCount(10);

        warningsArea = new TextArea();
        warningsArea.setEditable(false);
        warningsArea.setPromptText("Warning validazione/rendering");
        warningsArea.setPrefRowCount(4);

        renderButton = new Button("Renderizza anteprima");

        HBox editors = new HBox(12,
                createColumn("Template", templateArea),
                createColumn("Parametri (JSON)", parametersArea)
        );
        HBox previews = new HBox(12,
                createColumn("Markup Risolto", resolvedMarkupArea),
                createColumn("HTML Output", htmlPreviewArea)
        );

        getChildren().addAll(header, renderButton, editors, previews, new Label("Warnings"), warningsArea);
    }

    private VBox createColumn(String title, TextArea area) {
        Label label = new Label(title);
        VBox box = new VBox(6, label, area);
        HBox.setHgrow(box, Priority.ALWAYS);
        VBox.setVgrow(area, Priority.ALWAYS);
        box.setFillWidth(true);
        return box;
    }

    public AppHeader getHeader() {
        return header;
    }

    public TextArea getTemplateArea() {
        return templateArea;
    }

    public TextArea getParametersArea() {
        return parametersArea;
    }

    public TextArea getResolvedMarkupArea() {
        return resolvedMarkupArea;
    }

    public TextArea getHtmlPreviewArea() {
        return htmlPreviewArea;
    }

    public TextArea getWarningsArea() {
        return warningsArea;
    }

    public Button getRenderButton() {
        return renderButton;
    }
}
