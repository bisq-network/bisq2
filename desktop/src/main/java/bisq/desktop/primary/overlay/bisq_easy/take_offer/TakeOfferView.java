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

package bisq.desktop.primary.overlay.bisq_easy.take_offer;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.primary.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TakeOfferView extends NavigationView<VBox, TakeOfferModel, TakeOfferController> {
    public static final double POPUP_HEIGHT = OverlayModel.HEIGHT;
    public static final double TOP_PANE_HEIGHT = 55;
    public static final double BUTTON_HEIGHT = 32;
    public static final double BUTTON_BOTTOM = 40;
    public static final double CONTENT_HEIGHT = POPUP_HEIGHT - TOP_PANE_HEIGHT - BUTTON_HEIGHT - BUTTON_BOTTOM;
    private static final double OPACITY = 0.35;

    private final Button closeButton;
    private final List<Label> navigationProgressLabelList = new ArrayList<>();
    private final HBox progressBox;
    private final Button nextButton, backButton;
    private final VBox content;
    private final ChangeListener<Number> currentIndexListener;
    private final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;
    private Scene rootScene;

    public TakeOfferView(TakeOfferModel model, TakeOfferController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(POPUP_HEIGHT);

        Label review = getTopPaneLabel(Res.get("bisqEasy.takeOffer.review"));
        navigationProgressLabelList.add(review);

        closeButton = BisqIconButton.createIconButton("close");

        progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setId("onboarding-top-panel");
        progressBox.setMinHeight(TOP_PANE_HEIGHT);
        progressBox.setMaxHeight(TOP_PANE_HEIGHT);
        progressBox.setPadding(new Insets(0, 20, 0, 50));
        progressBox.getChildren().addAll(Spacer.fillHBox(),
                review,
                Spacer.fillHBox(), closeButton);

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
        root.getChildren().addAll(progressBox, content, Spacer.fillVBox(), buttons);

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

        if (model.isSettlementVisible()) {
            Label settlement = getTopPaneLabel(Res.get("bisqEasy.takeOffer.method"));
            navigationProgressLabelList.add(0, settlement);
            progressBox.getChildren().add(1, getSeparator());
            progressBox.getChildren().add(1, settlement);
        }
        if (model.isAmountVisible()) {
            Label amount = getTopPaneLabel(Res.get("bisqEasy.takeOffer.amount"));
            navigationProgressLabelList.add(0, amount);
            progressBox.getChildren().add(1, getSeparator());
            progressBox.getChildren().add(1, amount);
        }

        nextButton.textProperty().bind(model.getNextButtonText());
        backButton.textProperty().bind(model.getBackButtonText());

        nextButton.visibleProperty().bind(model.getNextButtonVisible());
        nextButton.managedProperty().bind(model.getNextButtonVisible());
        backButton.visibleProperty().bind(model.getBackButtonVisible());
        backButton.managedProperty().bind(model.getBackButtonVisible());
        closeButton.visibleProperty().bind(model.getCloseButtonVisible());
        nextButton.disableProperty().bind(model.getNextButtonDisabled());


        model.getCurrentIndex().addListener(currentIndexListener);
        model.getView().addListener(viewChangeListener);
        applyProgress(model.getCurrentIndex().get(), false);

        nextButton.setOnAction(e -> controller.onNext());
        backButton.setOnAction(evt -> controller.onBack());
        closeButton.setOnAction(e -> controller.onClose());
        rootScene = root.getScene();
        rootScene.setOnKeyReleased(keyEvent -> {
            KeyHandlerUtil.handleShutDownKeyEvent(keyEvent, controller::onQuit);
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, controller::onClose);
            KeyHandlerUtil.handleDevModeKeyEvent(keyEvent);
        });
    }

    @Override
    protected void onViewDetached() {
        nextButton.textProperty().unbind();
        backButton.textProperty().unbind();
        closeButton.textProperty().unbind();

        nextButton.visibleProperty().unbind();
        nextButton.managedProperty().unbind();
        backButton.visibleProperty().unbind();
        backButton.managedProperty().unbind();
        closeButton.visibleProperty().unbind();
        nextButton.disableProperty().unbind();

        nextButton.setOnAction(null);
        backButton.setOnAction(null);
        closeButton.setOnAction(null);
        rootScene.setOnKeyReleased(null);
        model.getCurrentIndex().removeListener(currentIndexListener);
        model.getView().removeListener(viewChangeListener);
    }

    private Separator getSeparator() {
        Separator line = new Separator();
        line.setPrefWidth(30);
        return line;
    }

    private Label getTopPaneLabel(String text) {
        Label label = new Label(text.toUpperCase());
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().addAll("bisq-text-14");

        label.setOpacity(OPACITY);
        return label;
    }

    private void applyProgress(int progressIndex, boolean delay) {
        if (progressIndex < navigationProgressLabelList.size()) {
            navigationProgressLabelList.forEach(label -> label.setOpacity(OPACITY));
            Label label = navigationProgressLabelList.get(progressIndex);
            if (delay) {
                UIScheduler.run(() -> Transitions.fade(label, OPACITY, 1, Transitions.DEFAULT_DURATION / 2))
                        .after(Transitions.DEFAULT_DURATION / 2);
            } else {
                label.setOpacity(1);
            }
        }
    }
}
