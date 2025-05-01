/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.components.controls;

import bisq.desktop.common.threading.UIScheduler;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Carousel extends BorderPane {
    private final List<Node> items = new ArrayList<>();
    private final IntegerProperty currentIndex = new SimpleIntegerProperty(0);
    private final BooleanProperty isPlaying = new SimpleBooleanProperty(false);
    @Getter
    private final BooleanProperty showBottomDots = new SimpleBooleanProperty(true);
    private final StackPane itemStackPane;
    private final Button leftArrowButton;
    private final Button rightArrowButton;
    @Getter
    private final HBox indicatorsHBox;

    @Setter
    private Duration transitionDuration = Duration.millis(400);
    @Setter
    private Duration displayDuration = Duration.millis(3000);

    private UIScheduler transitionScheduler;
    private boolean isTransitioning = false;
    private Node currentNode;
    private Node nextNode;

    public Carousel() {
        this(new ArrayList<>());
    }

    public Carousel(List<Node> items) {
        getStyleClass().add("carousel");

        Rectangle clipRectangle = new Rectangle();

        itemStackPane = new StackPane();
        itemStackPane.getStyleClass().add("carousel-item-container");

        clipRectangle.widthProperty().bind(itemStackPane.widthProperty());
        clipRectangle.heightProperty().bind(itemStackPane.heightProperty());
        itemStackPane.setClip(clipRectangle);

        leftArrowButton = new Button("❮");
        leftArrowButton.getStyleClass().addAll("carousel-nav-arrow");
        leftArrowButton.setOnAction(e -> previous());

        rightArrowButton = new Button("❯");
        rightArrowButton.getStyleClass().addAll("carousel-nav-arrow");
        rightArrowButton.setOnAction(e -> next());

        indicatorsHBox = new HBox(6);
        indicatorsHBox.setAlignment(Pos.CENTER);
        indicatorsHBox.getStyleClass().add("carousel-indicators");

        VBox contentBox = new VBox(5);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.getChildren().addAll(itemStackPane, indicatorsHBox);

        setCenter(contentBox);
        setLeft(leftArrowButton);
        setRight(rightArrowButton);

        BorderPane.setAlignment(leftArrowButton, Pos.CENTER);
        BorderPane.setAlignment(rightArrowButton, Pos.CENTER);

        indicatorsHBox.visibleProperty().bind(showBottomDots);
        indicatorsHBox.managedProperty().bind(showBottomDots);

        if (!items.isEmpty()) {
            for (Node item : items) {
                addItem(item);
            }
        }

        currentIndex.addListener((obs, oldVal, newVal) -> updateIndicators());
    }

    public void addItem(Node item) {
        items.add(item);

        // If this is the first item, size the carousel according to the item dimensions
        if (items.size() == 1) {
            if (item instanceof ImageView imageView) {
                double width = imageView.getFitWidth() > 0 ? imageView.getFitWidth() : imageView.getImage().getWidth();
                double height = imageView.getFitHeight() > 0 ? imageView.getFitHeight() : imageView.getImage().getHeight();

                itemStackPane.setPrefWidth(width);
                itemStackPane.setPrefHeight(height);
                itemStackPane.setMaxWidth(width);
                itemStackPane.setMaxHeight(height);
                itemStackPane.setMinWidth(width);
                itemStackPane.setMinHeight(height);
            } else if (item instanceof Region region) {
                if (region.getPrefWidth() > 0 && region.getPrefHeight() > 0) {
                    itemStackPane.setPrefWidth(region.getPrefWidth());
                    itemStackPane.setPrefHeight(region.getPrefHeight());
                    itemStackPane.setMaxWidth(region.getPrefWidth());
                    itemStackPane.setMaxHeight(region.getPrefHeight());
                    itemStackPane.setMinWidth(region.getPrefWidth());
                    itemStackPane.setMinHeight(region.getPrefHeight());
                }
            }

            setupInitialItem();
        }

        updateIndicators();
        updateArrowVisibility();
    }

    public void start() {
        if (items.size() <= 1) {
            return;
        }

        isPlaying.set(true);
        scheduleNextTransition();
    }

    public void stop() {
        isPlaying.set(false);

        if (transitionScheduler != null) {
            transitionScheduler.stop();
            transitionScheduler = null;
        }
    }

    public void next() {
        if (items.size() <= 1 || isTransitioning) {
            return;
        }

        int nextIndex = (currentIndex.get() + 1) % items.size();
        transitionToIndex(nextIndex, true);
    }

    public void previous() {
        if (items.size() <= 1 || isTransitioning) {
            return;
        }

        int prevIndex = (currentIndex.get() - 1 + items.size()) % items.size();
        transitionToIndex(prevIndex, false);
    }

    public void goToIndex(int index) {
        if (index < 0 || index >= items.size() || index == currentIndex.get() || isTransitioning) {
            return;
        }

        transitionToIndex(index, index > currentIndex.get());
    }

    private void setupInitialItem() {
        if (items.isEmpty()) {
            return;
        }

        itemStackPane.getChildren().clear();
        currentNode = items.get(currentIndex.get());
        itemStackPane.getChildren().add(currentNode);
    }

    private void updateIndicators() {
        indicatorsHBox.getChildren().clear();

        if (items.size() <= 1) {
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            StackPane dot = new StackPane();
            dot.getStyleClass().add("carousel-indicator");

            if (i == currentIndex.get()) {
                dot.getStyleClass().add("active");
            }

            final int index = i;
            dot.setOnMouseClicked(e -> goToIndex(index));

            indicatorsHBox.getChildren().add(dot);
        }
    }

    private void updateArrowVisibility() {
        boolean visible = items.size() > 1;
        leftArrowButton.setVisible(visible);
        leftArrowButton.setManaged(visible);
        rightArrowButton.setVisible(visible);
        rightArrowButton.setManaged(visible);
    }

    private void transitionToIndex(int targetIndex, boolean goingForward) {
        if (isTransitioning || items.isEmpty() || targetIndex == currentIndex.get()) {
            return;
        }

        isTransitioning = true;

        if (transitionScheduler != null) {
            transitionScheduler.stop();
            transitionScheduler = null;
        }

        Node currentItem = items.get(currentIndex.get());
        Node targetItem = items.get(targetIndex);

        nextNode = targetItem;
        nextNode.setTranslateX(goingForward ? itemStackPane.getWidth() : -itemStackPane.getWidth());
        itemStackPane.getChildren().add(nextNode);

        ParallelTransition transition = createSlideTransition(currentItem, targetItem, goingForward);

        transition.setOnFinished(e -> {
            currentIndex.set(targetIndex);
            itemStackPane.getChildren().clear();
            currentNode = nextNode;
            itemStackPane.getChildren().add(currentNode);
            nextNode = null;
            isTransitioning = false;

            if (isPlaying.get()) {
                scheduleNextTransition();
            }
        });

        transition.play();
    }

    private ParallelTransition createSlideTransition(Node currentItem, Node targetItem, boolean goingForward) {
        TranslateTransition exitTransition = new TranslateTransition(transitionDuration, currentItem);
        exitTransition.setToX(goingForward ? -itemStackPane.getWidth() : itemStackPane.getWidth());

        TranslateTransition enterTransition = new TranslateTransition(transitionDuration, targetItem);
        enterTransition.setToX(0);

        return new ParallelTransition(exitTransition, enterTransition);
    }

    private void scheduleNextTransition() {
        if (transitionScheduler != null) {
            transitionScheduler.stop();
        }

        transitionScheduler = UIScheduler.run(this::next).after((long) displayDuration.toMillis());
    }
}