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

package bisq.desktop.main.content.bisq_easy.trade_wizard;

import bisq.common.data.Triple;
import bisq.desktop.common.Layout;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.animation.FadeTransition;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
public class TradeWizardView extends NavigationView<VBox, TradeWizardModel, TradeWizardController> {
    public static final double POPUP_HEIGHT = OverlayModel.HEIGHT;
    public static final double TOP_PANE_HEIGHT = 55;
    public static final double BUTTON_HEIGHT = 32;
    public static final double BUTTON_BOTTOM = 40;
    public static final double CONTENT_HEIGHT = POPUP_HEIGHT - TOP_PANE_HEIGHT - BUTTON_HEIGHT - BUTTON_BOTTOM;
    private static final double OPACITY = 0.35;

    private final List<Label> progressLabelList;
    private final HBox progressItemsBox;
    private final Button nextButton, backButton, closeButton;
    private final VBox content;
    private final ChangeListener<Number> currentIndexListener;
    private final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;
    private Label priceProgressItemLabel, takeOfferProgressItem;
    private Region priceProgressItemLine;
    private Subscription priceProgressItemVisiblePin;
    private Region takeOfferProgressLine;
    private UIScheduler progressLabelAnimationScheduler;
    private FadeTransition progressLabelAnimation;

    public TradeWizardView(TradeWizardModel model, TradeWizardController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(POPUP_HEIGHT);

        Triple<HBox, Button, List<Label>> triple = getProgressItems();
        progressItemsBox = triple.getFirst();
        closeButton = triple.getSecond();
        progressLabelList = triple.getThird();

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        backButton = new Button(Res.get("action.back"));
        HBox buttons = new HBox(10, backButton, nextButton);
        buttons.setAlignment(Pos.CENTER);

        content = new VBox();
        content.setMinHeight(CONTENT_HEIGHT);
        content.setMaxHeight(CONTENT_HEIGHT);
        content.setAlignment(Pos.CENTER);

        VBox.setMargin(buttons, new Insets(0, 0, BUTTON_BOTTOM, 0));
        VBox.setMargin(content, new Insets(0, 40, 0, 40));
        root.getChildren().addAll(progressItemsBox, content, Spacer.fillVBox(), buttons);

        viewChangeListener = (observable, oldValue, newValue) -> {
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
        };

        currentIndexListener = (observable, oldValue, newValue) -> applyProgress(newValue.intValue(), true);
    }

    @Override
    protected void onViewAttached() {
        takeOfferProgressItem.setVisible(!model.isCreateOfferMode());
        takeOfferProgressItem.setManaged(!model.isCreateOfferMode());
        takeOfferProgressLine.setVisible(!model.isCreateOfferMode());
        takeOfferProgressLine.setManaged(!model.isCreateOfferMode());

        nextButton.textProperty().bind(model.getNextButtonText());
        nextButton.visibleProperty().bind(model.getNextButtonVisible());
        nextButton.managedProperty().bind(model.getNextButtonVisible());
        nextButton.disableProperty().bind(model.getNextButtonDisabled());

        backButton.textProperty().bind(model.getBackButtonText());
        backButton.visibleProperty().bind(model.getBackButtonVisible());
        backButton.managedProperty().bind(model.getBackButtonVisible());
        backButton.defaultButtonProperty().bind(model.getIsBackButtonHighlighted());
        backButton.setFocusTraversable(false);

        closeButton.visibleProperty().bind(model.getCloseButtonVisible());
        closeButton.setFocusTraversable(false);

        model.getCurrentIndex().addListener(currentIndexListener);
        model.getView().addListener(viewChangeListener);

        priceProgressItemVisiblePin = EasyBind.subscribe(model.getPriceProgressItemVisible(), isVisible -> {
            if (isVisible) {
                if (!progressItemsBox.getChildren().contains(priceProgressItemLine)) {
                    progressItemsBox.getChildren().add(5, priceProgressItemLine);
                }
                if (!progressItemsBox.getChildren().contains(priceProgressItemLabel)) {
                    progressItemsBox.getChildren().add(5, priceProgressItemLabel);
                }
                if (!progressLabelList.contains(priceProgressItemLabel)) {
                    progressLabelList.add(2, priceProgressItemLabel);
                }
            } else {
                progressItemsBox.getChildren().remove(priceProgressItemLine);
                progressItemsBox.getChildren().remove(priceProgressItemLabel);
                progressLabelList.remove(priceProgressItemLabel);
            }
        });

        nextButton.setOnAction(e -> controller.onNext());
        backButton.setOnAction(evt -> controller.onBack());
        closeButton.setOnAction(e -> controller.onClose());
        root.setOnKeyPressed(controller::onKeyPressed);

        applyProgress(model.getCurrentIndex().get(), false);
    }

    @Override
    protected void onViewDetached() {
        nextButton.textProperty().unbind();
        nextButton.visibleProperty().unbind();
        nextButton.managedProperty().unbind();
        nextButton.disableProperty().unbind();

        backButton.textProperty().unbind();
        backButton.visibleProperty().unbind();
        backButton.managedProperty().unbind();
        backButton.defaultButtonProperty().unbind();

        closeButton.visibleProperty().unbind();

        model.getCurrentIndex().removeListener(currentIndexListener);
        model.getView().removeListener(viewChangeListener);

        priceProgressItemVisiblePin.unsubscribe();

        nextButton.setOnAction(null);
        backButton.setOnAction(null);
        closeButton.setOnAction(null);
        root.setOnKeyPressed(null);

        if (progressLabelAnimationScheduler != null) {
            progressLabelAnimationScheduler.stop();
            progressLabelAnimationScheduler = null;
        }
        if (progressLabelAnimation != null) {
            progressLabelAnimation.stop();
            progressLabelAnimation = null;
        }
    }

    private Triple<HBox, Button, List<Label>> getProgressItems() {
        Label directionAndMarket = createAndGetProgressLabel(Res.get("bisqEasy.tradeWizard.progress.directionAndMarket"));
        priceProgressItemLabel = createAndGetProgressLabel(Res.get("bisqEasy.tradeWizard.progress.price"));
        priceProgressItemLine = getHLine();
        Label amount = createAndGetProgressLabel(Res.get("bisqEasy.tradeWizard.progress.amount"));
        Label paymentMethods = createAndGetProgressLabel(Res.get("bisqEasy.tradeWizard.progress.paymentMethods"));
        takeOfferProgressItem = createAndGetProgressLabel(Res.get("bisqEasy.tradeWizard.progress.takeOffer"));
        takeOfferProgressLine = getHLine();
        Label review = createAndGetProgressLabel(Res.get("bisqEasy.tradeWizard.progress.review"));

        Button closeButton = BisqIconButton.createIconButton("close");

        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.CENTER);
        hBox.setId("onboarding-top-panel");
        hBox.setMinHeight(TOP_PANE_HEIGHT);
        hBox.setMaxHeight(TOP_PANE_HEIGHT);
        hBox.setPadding(new Insets(0, 20, 0, 50));

        hBox.getChildren().addAll(Spacer.fillHBox(),
                directionAndMarket,
                getHLine(),
                amount,
                getHLine(),
                paymentMethods,
                takeOfferProgressLine,
                takeOfferProgressItem,
                getHLine(),
                review,
                Spacer.fillHBox(),
                closeButton);

        return new Triple<>(hBox, closeButton, new ArrayList<>(List.of(directionAndMarket, amount, paymentMethods, takeOfferProgressItem, review)));
    }

    private Region getHLine() {
        Region line = Layout.hLine();
        line.setPrefWidth(30);
        return line;
    }

    private Label createAndGetProgressLabel(String text) {
        Label label = new Label(text.toUpperCase());
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().add("bisq-text-14");
        label.setOpacity(OPACITY);
        return label;
    }

    private void applyProgress(int progressIndex, boolean delay) {
        if (progressIndex < progressLabelList.size()) {
            progressLabelList.forEach(label -> label.setOpacity(OPACITY));

            if (progressLabelAnimation != null) {
                progressLabelAnimation.stop();
                progressLabelAnimation.getNode().setOpacity(OPACITY);
            }
            Label label = progressLabelList.get(progressIndex);
            if (delay) {
                if (progressLabelAnimationScheduler != null) {
                    progressLabelAnimationScheduler.stop();
                }
                progressLabelAnimationScheduler = UIScheduler.run(() -> progressLabelAnimation = Transitions.fade(label, OPACITY, 1, Transitions.DEFAULT_DURATION / 2))
                        .after(Transitions.DEFAULT_DURATION / 2);
            } else {
                label.setOpacity(1);
            }
        }
    }
}
