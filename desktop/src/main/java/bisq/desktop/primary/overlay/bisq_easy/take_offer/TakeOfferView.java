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
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

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

    private final List<Label> navigationProgressLabelList = new ArrayList<>();
    private final HBox progressBox, topPane;
    private final Button nextButton, takeOfferButton, backButton, closeButton;
    private final VBox content;
    private final ChangeListener<Number> currentIndexListener;
    private final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;
    private Scene rootScene;
    private Subscription showProgressBoxPin, takeOfferButtonVisiblePin;

    public TakeOfferView(TakeOfferModel model, TakeOfferController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(POPUP_HEIGHT);

        Label review = getTopPaneLabel(Res.get("bisqEasy.takeOffer.progress.review"));
        navigationProgressLabelList.add(review);

        progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setMinHeight(TOP_PANE_HEIGHT);
        progressBox.setMaxHeight(TOP_PANE_HEIGHT);
        progressBox.setPadding(new Insets(0, 20, 0, 50));
        progressBox.getChildren().add(review);

        closeButton = BisqIconButton.createIconButton("close");

        topPane = new HBox();
        topPane.setAlignment(Pos.CENTER);
        topPane.setStyle("-fx-background-color: -bisq-grey-23");
        topPane.setMinHeight(TOP_PANE_HEIGHT);
        topPane.setMaxHeight(TOP_PANE_HEIGHT);
        topPane.setPadding(new Insets(0, 20, 0, 50));
        topPane.getChildren().addAll(Spacer.fillHBox(),
                progressBox,
                Spacer.fillHBox(),
                closeButton);

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        takeOfferButton = new Button(Res.get("bisqEasy.takeOffer.review.takeOffer"));
        takeOfferButton.setDefaultButton(true);

        backButton = new Button(Res.get("action.back"));
        HBox buttons = new HBox(10, backButton, nextButton, takeOfferButton);
        buttons.setAlignment(Pos.CENTER);

        content = new VBox();
        content.setMinHeight(CONTENT_HEIGHT);
        content.setMaxHeight(CONTENT_HEIGHT);
        content.setAlignment(Pos.CENTER);

        VBox.setMargin(buttons, new Insets(0, 0, BUTTON_BOTTOM, 0));
        VBox.setMargin(content, new Insets(0, 40, 0, 40));
        root.getChildren().addAll(topPane, content, Spacer.fillVBox(), buttons);

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
        if (model.isPaymentMethodVisible()) {
            Label paymentMethod = getTopPaneLabel(Res.get("bisqEasy.takeOffer.progress.method"));
            navigationProgressLabelList.add(0, paymentMethod);
            progressBox.getChildren().add(0, getSeparator());
            progressBox.getChildren().add(0, paymentMethod);
        }
        if (model.isAmountVisible()) {
            Label amount = getTopPaneLabel(Res.get("bisqEasy.takeOffer.progress.amount"));
            navigationProgressLabelList.add(0, amount);
            progressBox.getChildren().add(0, getSeparator());
            progressBox.getChildren().add(0, amount);
        }
        if (model.isPriceVisible()) {
            Label price = getTopPaneLabel(Res.get("bisqEasy.takeOffer.progress.price"));
            navigationProgressLabelList.add(0, price);
            progressBox.getChildren().add(0, getSeparator());
            progressBox.getChildren().add(0, price);
        }

        nextButton.textProperty().bind(model.getNextButtonText());
        nextButton.disableProperty().bind(model.getNextButtonDisabled());
        nextButton.visibleProperty().bind(model.getNextButtonVisible());
        nextButton.managedProperty().bind(model.getNextButtonVisible());
        takeOfferButton.visibleProperty().bind(model.getTakeOfferButtonVisible());
        takeOfferButton.managedProperty().bind(model.getTakeOfferButtonVisible());
        backButton.textProperty().bind(model.getBackButtonText());
        backButton.visibleProperty().bind(model.getBackButtonVisible());
        backButton.managedProperty().bind(model.getBackButtonVisible());
        closeButton.visibleProperty().bind(model.getCloseButtonVisible());

        model.getCurrentIndex().addListener(currentIndexListener);
        model.getView().addListener(viewChangeListener);

        takeOfferButtonVisiblePin = EasyBind.subscribe(model.getTakeOfferButtonVisible(), takeOfferButtonVisible -> {
            if (takeOfferButtonVisible) {
                backButton.prefWidthProperty().bind(takeOfferButton.widthProperty());
            } else {
                backButton.prefWidthProperty().unbind();
                backButton.setPrefWidth(Region.USE_COMPUTED_SIZE);
            }
        });

        rootScene = root.getScene();
        rootScene.setOnKeyReleased(keyEvent -> {
            KeyHandlerUtil.handleShutDownKeyEvent(keyEvent, controller::onQuit);
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, controller::onClose);
            KeyHandlerUtil.handleDevModeKeyEvent(keyEvent);
        });

        if (!model.getShowProgressBox().get()) {
            progressBox.setOpacity(0);
            topPane.setStyle("-fx-background-color: transparent");
        }
        showProgressBoxPin = EasyBind.subscribe(model.getShowProgressBox(), showProgressBox -> {
            if (showProgressBox) {
                // VBox.setMargin(content, new Insets(0, 0, 0, 0));
                Transitions.fadeIn(progressBox, 200);
                topPane.setStyle("-fx-background-color: -bisq-grey-23");
            } else {
                // VBox.setMargin(content, new Insets(0, 40, 0, 40));
                Transitions.fadeOut(progressBox, 200);
                topPane.setStyle("-fx-background-color: transparent");
            }
        });

        nextButton.setOnAction(e -> controller.onNext());
        takeOfferButton.setOnAction(e -> controller.onTakeOffer());
        backButton.setOnAction(evt -> controller.onBack());
        closeButton.setOnAction(e -> controller.onClose());

        applyProgress(model.getCurrentIndex().get(), false);
    }

    @Override
    protected void onViewDetached() {
        nextButton.textProperty().unbind();
        nextButton.disableProperty().unbind();
        nextButton.visibleProperty().unbind();
        nextButton.managedProperty().unbind();
        takeOfferButton.visibleProperty().unbind();
        takeOfferButton.managedProperty().unbind();
        backButton.textProperty().unbind();
        backButton.visibleProperty().unbind();
        backButton.managedProperty().unbind();
        backButton.prefWidthProperty().unbind();
        closeButton.visibleProperty().unbind();


        model.getCurrentIndex().removeListener(currentIndexListener);
        model.getView().removeListener(viewChangeListener);

        showProgressBoxPin.unsubscribe();
        takeOfferButtonVisiblePin.unsubscribe();

        nextButton.setOnAction(null);
        takeOfferButton.setOnAction(null);
        backButton.setOnAction(null);
        closeButton.setOnAction(null);
        rootScene.setOnKeyReleased(null);
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
