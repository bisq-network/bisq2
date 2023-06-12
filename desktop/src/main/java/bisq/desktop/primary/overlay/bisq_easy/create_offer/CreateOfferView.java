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

package bisq.desktop.primary.overlay.bisq_easy.create_offer;

import bisq.common.data.Triple;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.primary.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CreateOfferView extends NavigationView<VBox, CreateOfferModel, CreateOfferController> {
    public static final double POPUP_HEIGHT = OverlayModel.HEIGHT;
    public static final double TOP_PANE_HEIGHT = 55;
    public static final double BUTTON_HEIGHT = 32;
    public static final double BUTTON_BOTTOM = 40;
    public static final double CONTENT_HEIGHT = POPUP_HEIGHT - TOP_PANE_HEIGHT - BUTTON_HEIGHT - BUTTON_BOTTOM;
    private static final double OPACITY = 0.35;

    private final Button closeButton;
    private final List<Label> progressLabelList;
    private final Button nextButton, backButton;
    private final VBox content;
    private final ChangeListener<Number> currentIndexListener;
    private final HBox progressItemsBox;
    private Scene rootScene;
    private Label priceProgressItemLabel;
    private Separator priceProgressItemSeparator;
    private Subscription priceProgressItemVisiblePin;

    public CreateOfferView(CreateOfferModel model, CreateOfferController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(POPUP_HEIGHT);

        Triple<HBox, Button, List<Label>> topPane = getProgressItems();
        progressItemsBox = topPane.getFirst();
        closeButton = topPane.getSecond();
        progressLabelList = topPane.getThird();

        nextButton = new Button(Res.get("next"));
        nextButton.setDefaultButton(true);

        backButton = new Button(Res.get("back"));
        HBox buttons = new HBox(10, backButton, nextButton);
        buttons.setAlignment(Pos.CENTER);

        content = new VBox();
        content.setMinHeight(CONTENT_HEIGHT);
        content.setMaxHeight(CONTENT_HEIGHT);
        content.setAlignment(Pos.CENTER);

        VBox.setMargin(buttons, new Insets(0, 0, BUTTON_BOTTOM, 0));
        root.getChildren().addAll(progressItemsBox, content, Spacer.fillVBox(), buttons);

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                childRoot.setMinHeight(CONTENT_HEIGHT);
                childRoot.setMaxHeight(CONTENT_HEIGHT);
                content.getChildren().setAll(childRoot);
                if (oldValue != null) {
                    if (model.isAnimateRightOut()) {
                        Transitions.transitRightOut(childRoot, oldValue.getRoot());
                    } else {
                        Transitions.transitLeftOut(childRoot, oldValue.getRoot());
                    }
                } else {
                    Transitions.fadeIn(childRoot);
                }
            } else {
                content.getChildren().clear();
            }
        });

        currentIndexListener = (observable, oldValue, newValue) -> applyProgress(newValue.intValue(), true);
    }

    @Override
    protected void onViewAttached() {
        nextButton.textProperty().bind(model.getNextButtonText());
        backButton.textProperty().bind(model.getBackButtonText());

        nextButton.visibleProperty().bind(model.getNextButtonVisible());
        nextButton.managedProperty().bind(model.getNextButtonVisible());
        backButton.visibleProperty().bind(model.getBackButtonVisible());
        backButton.managedProperty().bind(model.getBackButtonVisible());
        closeButton.visibleProperty().bind(model.getCloseButtonVisible());
        nextButton.disableProperty().bind(model.getNextButtonDisabled());

        model.getCurrentIndex().addListener(currentIndexListener);
        rootScene = root.getScene();
        rootScene.setOnKeyReleased(keyEvent -> {
            KeyHandlerUtil.handleShutDownKeyEvent(keyEvent, controller::onQuit);
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, controller::onClose);
            KeyHandlerUtil.handleDevModeKeyEvent(keyEvent);
        });
        priceProgressItemVisiblePin = EasyBind.subscribe(model.getPriceProgressItemVisible(), isVisible -> {
            if (isVisible) {
                progressItemsBox.getChildren().add(5, priceProgressItemSeparator);
                progressItemsBox.getChildren().add(5, priceProgressItemLabel);
                progressLabelList.add(2, priceProgressItemLabel);
            } else {
                progressItemsBox.getChildren().remove(priceProgressItemSeparator);
                progressItemsBox.getChildren().remove(priceProgressItemLabel);
                progressLabelList.remove(priceProgressItemLabel);
            }
        });

        nextButton.setOnAction(e -> controller.onNext());
        backButton.setOnAction(evt -> controller.onBack());
        closeButton.setOnAction(e -> controller.onClose());

        applyProgress(model.getCurrentIndex().get(), false);
    }

    @Override
    protected void onViewDetached() {
        nextButton.textProperty().unbind();
        backButton.textProperty().unbind();

        nextButton.visibleProperty().unbind();
        nextButton.managedProperty().unbind();
        backButton.visibleProperty().unbind();
        backButton.managedProperty().unbind();
        closeButton.visibleProperty().unbind();
        nextButton.disableProperty().unbind();

        model.getCurrentIndex().removeListener(currentIndexListener);

        priceProgressItemVisiblePin.unsubscribe();

        nextButton.setOnAction(null);
        backButton.setOnAction(null);
        closeButton.setOnAction(null);
        rootScene.setOnKeyReleased(null);
    }

    private Triple<HBox, Button, List<Label>> getProgressItems() {
        Label direction = createAndGetProgressLabel(Res.get("onboarding.navProgress.direction"));
        Label market = createAndGetProgressLabel(Res.get("onboarding.navProgress.market"));
        priceProgressItemLabel = createAndGetProgressLabel(Res.get("onboarding.navProgress.price"));
        priceProgressItemSeparator = createAndGetSeparator();
        Label amount = createAndGetProgressLabel(Res.get("onboarding.navProgress.amount"));
        Label method = createAndGetProgressLabel(Res.get("onboarding.navProgress.method"));
        Label complete = createAndGetProgressLabel(Res.get("onboarding.navProgress.review"));

        Button closeButton = BisqIconButton.createIconButton("close");

        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.CENTER);
        hBox.setId("onboarding-top-panel");
        hBox.setMinHeight(TOP_PANE_HEIGHT);
        hBox.setMaxHeight(TOP_PANE_HEIGHT);
        hBox.setPadding(new Insets(0, 20, 0, 50));
        hBox.getChildren().addAll(Spacer.fillHBox(),
                direction,
                createAndGetSeparator(),
                market,
                createAndGetSeparator(),
                amount,
                createAndGetSeparator(),
                method,
                createAndGetSeparator(),
                complete,
                Spacer.fillHBox(), closeButton);

        return new Triple<>(hBox, closeButton, new ArrayList<>(List.of(direction, market, amount, method, complete)));
    }

    private Separator createAndGetSeparator() {
        Separator line = new Separator();
        line.setPrefWidth(30);
        return line;
    }

    private Label createAndGetProgressLabel(String text) {
        Label label = new Label(text.toUpperCase());
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().addAll("bisq-text-14");

        label.setOpacity(OPACITY);
        return label;
    }

    private void applyProgress(int progressIndex, boolean delay) {
        if (progressIndex < progressLabelList.size()) {
            progressLabelList.forEach(label -> label.setOpacity(OPACITY));
            Label label = progressLabelList.get(progressIndex);
            if (delay) {
                UIScheduler.run(() -> Transitions.fade(label, OPACITY, 1, Transitions.DEFAULT_DURATION / 2))
                        .after(Transitions.DEFAULT_DURATION / 2);
            } else {
                label.setOpacity(1);
            }
        }
    }
}
