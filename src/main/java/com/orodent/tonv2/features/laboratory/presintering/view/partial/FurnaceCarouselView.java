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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FurnaceCarouselView extends VBox {

    private static final int DEFAULT_VISIBLE_CARDS = 3;
    private static final int MIN_VISIBLE_CARDS = 2;
    private static final int CARD_WIDTH = 165;
    private static final int CARD_GAP = 12;

    private final List<FurnaceCardData> furnaceCards = new ArrayList<>();
    private final Map<Integer, String> itemCodeById = new LinkedHashMap<>();

    private final StackPane carouselPane = new StackPane();
    private final HBox viewport = new HBox(14);

    private final StackPane leftPilePane = new StackPane();
    private final StackPane centerCardsPane = new StackPane();
    private final StackPane rightPilePane = new StackPane();

    private final Button leftArrow = new Button("<");
    private final Button rightArrow = new Button(">");

    private Timeline leftHoldTimeline;
    private Timeline rightHoldTimeline;

    private int firstVisibleCardIndex = 0;
    private int selectedCardIndex = -1;
    private Consumer<FurnaceSelection> onFurnaceSelectionChanged;

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
                        furnace.id(),
                        "Forno " + displayNumber,
                        new LinkedHashMap<>()
                ));
            }
        }

        firstVisibleCardIndex = 0;
        selectedCardIndex = -1;
        notifySelectionChanged();
        render();
    }

    public void setOnFurnaceSelectionChanged(Consumer<FurnaceSelection> onFurnaceSelectionChanged) {
        this.onFurnaceSelectionChanged = onFurnaceSelectionChanged;
    }

    public void setPlannedItems(Map<Integer, Map<Integer, Integer>> plannedByFurnace,
                                Map<Integer, String> itemCodeById) {
        this.itemCodeById.clear();
        if (itemCodeById != null) {
            this.itemCodeById.putAll(itemCodeById);
        }

        for (FurnaceCardData furnaceCard : furnaceCards) {
            furnaceCard.plannedItemQty().clear();
            Map<Integer, Integer> plannedItems = plannedByFurnace == null
                    ? null
                    : plannedByFurnace.get(furnaceCard.furnaceId());
            if (plannedItems == null) {
                continue;
            }

            for (Map.Entry<Integer, Integer> entry : plannedItems.entrySet()) {
                Integer qty = entry.getValue();
                if (qty == null || qty <= 0) {
                    continue;
                }
                furnaceCard.plannedItemQty().put(entry.getKey(), qty);
            }
        }
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
        centerCardsPane.setMinWidth(0);
        centerCardsPane.setPrefWidth(520);

        HBox.setHgrow(centerCardsPane, Priority.ALWAYS);

        viewport.setAlignment(Pos.CENTER);
        viewport.getChildren().addAll(leftPilePane, centerCardsPane, rightPilePane);
        centerCardsPane.widthProperty().addListener((obs, oldWidth, newWidth) -> render());

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

        int fullVisibleCards = getFullVisibleCards();

        if (furnaceCards.size() <= fullVisibleCards) {
            firstVisibleCardIndex = 0;
            HBox allCards = new HBox(CARD_GAP);
            allCards.setAlignment(Pos.CENTER);
            for (int index = 0; index < furnaceCards.size(); index++) {
                allCards.getChildren().add(createFullCard(furnaceCards.get(index), index));
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
        int rightHiddenCount = furnaceCards.size() - (firstVisibleCardIndex + fullVisibleCards);

        if (leftHiddenCount > 0) {
            leftPilePane.getChildren().add(createPile(leftHiddenCount, true));
        }

        HBox fullCardsRow = new HBox(CARD_GAP);
        fullCardsRow.setAlignment(Pos.CENTER);
        for (int index = firstVisibleCardIndex; index < firstVisibleCardIndex + fullVisibleCards; index++) {
            fullCardsRow.getChildren().add(createFullCard(furnaceCards.get(index), index));
        }
        centerCardsPane.getChildren().add(fullCardsRow);

        if (rightHiddenCount > 0) {
            rightPilePane.getChildren().add(createPile(rightHiddenCount, false));
        }

        boolean canGoLeft = leftHiddenCount > 0;
        boolean canGoRight = rightHiddenCount > 0;

        leftArrow.setVisible(canGoLeft);
        leftArrow.setManaged(canGoLeft);
        rightArrow.setVisible(canGoRight);
        rightArrow.setManaged(canGoRight);
    }

    private VBox createFullCard(FurnaceCardData furnace, int absoluteIndex) {
        VBox card = new VBox(8);
        card.setPrefWidth(CARD_WIDTH);
        card.setMinWidth(CARD_WIDTH);
        card.setPadding(new Insets(10));
        card.setStyle(buildCardStyle(selectedCardIndex == absoluteIndex));
        card.setOnMouseClicked(e -> selectCard(absoluteIndex));

        Label title = new Label(furnace.furnaceName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox itemsBox = new VBox(4);
        if (furnace.plannedItemQty().isEmpty()) {
            Label row = new Label("Nessun disco pianificato.");
            row.setStyle("-fx-font-size: 12px; -fx-opacity: 0.75;");
            itemsBox.getChildren().add(row);
        } else {
            for (Map.Entry<Integer, Integer> entry : furnace.plannedItemQty().entrySet()) {
                String itemCode = itemCodeById.getOrDefault(entry.getKey(), "Item " + entry.getKey());
                Label row = new Label(itemCode + " — " + entry.getValue() + " pz");
                row.setStyle("-fx-font-size: 12px; -fx-opacity: 0.92;");
                itemsBox.getChildren().add(row);
            }
        }

        card.getChildren().addAll(title, itemsBox);
        return card;
    }

    private String buildCardStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color: rgba(56, 189, 248, 0.36);"
                    + "-fx-border-color: rgba(14, 165, 233, 1.0);"
                    + "-fx-border-width: 2;"
                    + "-fx-border-radius: 10; -fx-background-radius: 10;";
        }

        return "-fx-background-color: rgba(56, 189, 248, 0.18);"
                + "-fx-border-color: rgba(125, 211, 252, 0.9);"
                + "-fx-border-radius: 10; -fx-background-radius: 10;";
    }

    private void selectCard(int absoluteIndex) {
        if (absoluteIndex < 0 || absoluteIndex >= furnaceCards.size() || selectedCardIndex == absoluteIndex) {
            return;
        }

        selectedCardIndex = absoluteIndex;
        notifySelectionChanged();
        render();
    }

    private void notifySelectionChanged() {
        if (onFurnaceSelectionChanged == null) {
            return;
        }

        if (selectedCardIndex < 0 || selectedCardIndex >= furnaceCards.size()) {
            onFurnaceSelectionChanged.accept(null);
            return;
        }

        FurnaceCardData selected = furnaceCards.get(selectedCardIndex);
        onFurnaceSelectionChanged.accept(new FurnaceSelection(selected.furnaceId(), selected.furnaceName()));
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
        int min = 0;
        int max = furnaceCards.size() - getFullVisibleCards();
        return Math.max(min, Math.min(max, candidate));
    }

    private int getFullVisibleCards() {
        double availableWidth = centerCardsPane.getWidth();
        if (availableWidth <= 0) {
            return Math.max(MIN_VISIBLE_CARDS, DEFAULT_VISIBLE_CARDS);
        }

        int cardsFromWidth = (int) Math.floor((availableWidth + CARD_GAP) / (CARD_WIDTH + CARD_GAP));
        return Math.max(MIN_VISIBLE_CARDS, cardsFromWidth);
    }

    private record FurnaceCardData(int furnaceId, String furnaceName, Map<Integer, Integer> plannedItemQty) {
    }

    public record FurnaceSelection(int furnaceId, String furnaceName) {
    }
}
