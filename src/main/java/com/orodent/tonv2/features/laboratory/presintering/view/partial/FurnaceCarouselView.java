package com.orodent.tonv2.features.laboratory.presintering.view.partial;

import com.orodent.tonv2.core.database.model.Furnace;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class FurnaceCarouselView extends VBox {

    private static final int FULL_VISIBLE_CARDS = 3;

    private final List<FurnaceCardData> furnaceCards = new ArrayList<>();

    private final StackPane carouselPane = new StackPane();
    private final HBox viewport = new HBox(14);

    private final StackPane leftPilePane = new StackPane();
    private final StackPane centerCardsPane = new StackPane();
    private final StackPane rightPilePane = new StackPane();

    private final Button leftArrow = new Button("<");
    private final Button rightArrow = new Button(">");

    private Timeline leftHoldTimeline;
    private Timeline rightHoldTimeline;

    private int firstVisibleCardIndex = 1;

    public FurnaceCarouselView() {
        buildUi();
        render();
    }

    public void setFurnaces(List<Furnace> furnaces) {
        furnaceCards.clear();

        if (furnaces != null) {
            for (Furnace furnace : furnaces) {
                String displayNumber = furnace.number() == null || furnace.number().isBlank()
                        ? String.valueOf(furnace.id())
                        : furnace.number();
                furnaceCards.add(new FurnaceCardData(
                        "Forno " + displayNumber,
                        buildPlaceholderItems(displayNumber)
                ));
            }
        }

        firstVisibleCardIndex = 1;
        render();
    }

    private void buildUi() {
        setSpacing(10);
        setPadding(new Insets(8, 0, 0, 0));

        Label sectionTitle = new Label("Guida forni");
        sectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label sectionHint = new Label("Scorri i forni per confrontare combinazioni item/temperatura prima di assegnare.");
        sectionHint.setStyle("-fx-opacity: 0.85;");

        leftPilePane.setMinWidth(95);
        leftPilePane.setPrefWidth(95);
        rightPilePane.setMinWidth(95);
        rightPilePane.setPrefWidth(95);
        centerCardsPane.setMinWidth(520);

        HBox.setHgrow(centerCardsPane, Priority.ALWAYS);

        viewport.setAlignment(Pos.CENTER);
        viewport.getChildren().addAll(leftPilePane, centerCardsPane, rightPilePane);

        carouselPane.getChildren().add(viewport);
        carouselPane.setPadding(new Insets(8, 4, 8, 4));

        leftArrow.getStyleClass().add("header-button");
        rightArrow.getStyleClass().add("header-button");
        leftArrow.setFocusTraversable(false);
        rightArrow.setFocusTraversable(false);

        StackPane.setAlignment(leftArrow, Pos.CENTER_LEFT);
        StackPane.setAlignment(rightArrow, Pos.CENTER_RIGHT);
        StackPane.setMargin(leftArrow, new Insets(0, 0, 0, 8));
        StackPane.setMargin(rightArrow, new Insets(0, 8, 0, 0));

        carouselPane.getChildren().addAll(leftArrow, rightArrow);

        attachArrowBehaviour(leftArrow, -1);
        attachArrowBehaviour(rightArrow, 1);

        getChildren().addAll(sectionTitle, sectionHint, carouselPane);
    }

    private void render() {
        leftPilePane.getChildren().clear();
        centerCardsPane.getChildren().clear();
        rightPilePane.getChildren().clear();

        if (furnaceCards.isEmpty()) {
            leftArrow.setVisible(false);
            rightArrow.setVisible(false);
            leftArrow.setManaged(false);
            rightArrow.setManaged(false);

            Label emptyLabel = new Label("Nessun forno disponibile.");
            emptyLabel.setStyle("-fx-opacity: 0.85;");
            centerCardsPane.getChildren().add(emptyLabel);
            return;
        }

        if (furnaceCards.size() <= 5) {
            firstVisibleCardIndex = 0;
            HBox allCards = new HBox(12);
            allCards.setAlignment(Pos.CENTER);
            for (FurnaceCardData furnace : furnaceCards) {
                allCards.getChildren().add(createFullCard(furnace));
            }
            centerCardsPane.getChildren().add(allCards);
            leftArrow.setVisible(false);
            rightArrow.setVisible(false);
            leftArrow.setManaged(false);
            rightArrow.setManaged(false);
            return;
        }

        firstVisibleCardIndex = clampFirstVisibleIndex(firstVisibleCardIndex);

        int leftHiddenCount = firstVisibleCardIndex;
        int rightHiddenCount = furnaceCards.size() - (firstVisibleCardIndex + FULL_VISIBLE_CARDS);

        if (leftHiddenCount > 0) {
            leftPilePane.getChildren().add(createPile(leftHiddenCount, true));
        }

        HBox fullCardsRow = new HBox(12);
        fullCardsRow.setAlignment(Pos.CENTER);
        for (int index = firstVisibleCardIndex; index < firstVisibleCardIndex + FULL_VISIBLE_CARDS; index++) {
            fullCardsRow.getChildren().add(createFullCard(furnaceCards.get(index)));
        }
        centerCardsPane.getChildren().add(fullCardsRow);

        if (rightHiddenCount > 0) {
            rightPilePane.getChildren().add(createPile(rightHiddenCount, false));
        }

        boolean canGoLeft = firstVisibleCardIndex > 1;
        boolean canGoRight = firstVisibleCardIndex < furnaceCards.size() - 4;

        leftArrow.setVisible(canGoLeft);
        leftArrow.setManaged(canGoLeft);
        rightArrow.setVisible(canGoRight);
        rightArrow.setManaged(canGoRight);
    }

    private VBox createFullCard(FurnaceCardData furnace) {
        VBox card = new VBox(8);
        card.setPrefWidth(165);
        card.setMinWidth(165);
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: rgba(56, 189, 248, 0.18);"
                        + "-fx-border-color: rgba(125, 211, 252, 0.9);"
                        + "-fx-border-radius: 10; -fx-background-radius: 10;"
        );

        Label title = new Label(furnace.furnaceName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox itemsBox = new VBox(4);
        for (FurnaceItemData item : furnace.items()) {
            Label row = new Label(item.itemCode() + " — " + item.temperature() + "°C");
            row.setStyle("-fx-font-size: 12px; -fx-opacity: 0.92;");
            itemsBox.getChildren().add(row);
        }

        card.getChildren().addAll(title, itemsBox);
        return card;
    }

    private StackPane createPile(int hiddenCards, boolean leftDirection) {
        StackPane pile = new StackPane();
        pile.setMinWidth(90);
        pile.setPrefWidth(90);

        int visibleSlices = Math.min(hiddenCards, 4);
        for (int i = visibleSlices - 1; i >= 0; i--) {
            VBox slice = new VBox();
            slice.setPrefSize(70, 132);
            slice.setStyle(
                    "-fx-background-color: rgba(148, 163, 184, 0.24);"
                            + "-fx-border-color: rgba(203, 213, 225, 0.6);"
                            + "-fx-border-radius: 8; -fx-background-radius: 8;"
            );
            double offset = i * 8;
            slice.setTranslateX(leftDirection ? -offset : offset);
            pile.getChildren().add(slice);
        }

        Label badge = new Label(hiddenCards + " forni");
        badge.setStyle("-fx-font-size: 11px; -fx-opacity: 0.85;");
        StackPane.setAlignment(badge, Pos.BOTTOM_CENTER);
        StackPane.setMargin(badge, new Insets(0, 0, 6, 0));
        pile.getChildren().add(badge);

        return pile;
    }

    private void attachArrowBehaviour(Button arrow, int direction) {
        arrow.setOnAction(e -> shiftBy(direction));

        arrow.setOnMousePressed(e -> {
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(320), keyFrame -> shiftBy(direction)));
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.playFromStart();

            if (direction < 0) {
                stopTimeline(leftHoldTimeline);
                leftHoldTimeline = timeline;
            } else {
                stopTimeline(rightHoldTimeline);
                rightHoldTimeline = timeline;
            }
        });

        arrow.setOnMouseReleased(e -> stopHold(direction));
        arrow.setOnMouseExited(e -> stopHold(direction));
    }

    private void stopHold(int direction) {
        if (direction < 0) {
            stopTimeline(leftHoldTimeline);
            leftHoldTimeline = null;
            return;
        }
        stopTimeline(rightHoldTimeline);
        rightHoldTimeline = null;
    }

    private void stopTimeline(Timeline timeline) {
        if (timeline != null) {
            timeline.stop();
        }
    }

    private void shiftBy(int direction) {
        int nextIndex = clampFirstVisibleIndex(firstVisibleCardIndex + direction);
        if (nextIndex != firstVisibleCardIndex) {
            firstVisibleCardIndex = nextIndex;
            render();
        }
    }

    private int clampFirstVisibleIndex(int candidate) {
        int min = 1;
        int max = furnaceCards.size() - 4;
        return Math.max(min, Math.min(max, candidate));
    }

    private List<FurnaceItemData> buildPlaceholderItems(String furnaceNumber) {
        int seed = Math.abs(furnaceNumber.hashCode());
        return List.of(
                new FurnaceItemData("Item A" + furnaceNumber, 1480 + (seed % 5) * 10),
                new FurnaceItemData("Item B" + furnaceNumber, 1500 + (seed % 4) * 12),
                new FurnaceItemData("Item C" + furnaceNumber, 1520 + (seed % 3) * 15)
        );
    }

    private record FurnaceCardData(String furnaceName, List<FurnaceItemData> items) {
    }

    private record FurnaceItemData(String itemCode, int temperature) {
    }
}
