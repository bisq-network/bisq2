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

package bisq.desktop.main.content.mu_sig.create_offer;
import bisq.desktop.common.Layout;
import bisq.desktop.common.ManagedDuration;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.animation.FadeTransition;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MuSigCreateOfferView extends NavigationView<VBox, MuSigCreateOfferModel, MuSigCreateOfferController> {
    public static final double POPUP_HEIGHT = OverlayModel.HEIGHT;
    public static final double TOP_PANE_HEIGHT = 55;
    public static final double BUTTON_HEIGHT = 32;
    public static final double BUTTON_BOTTOM = 40;
    public static final double CONTENT_HEIGHT = POPUP_HEIGHT - TOP_PANE_HEIGHT - BUTTON_HEIGHT - BUTTON_BOTTOM;
    private static final double OPACITY = 0.35;

    private final List<Label> progressLabelList = new ArrayList<>();
    private final HBox progressHeaderBox;
    private final Button nextButton, backButton, closeButton;
    private final VBox content;
    private final ChangeListener<Number> currentIndexListener;
    private final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;
    private final ListChangeListener<NavigationTarget> childTargetsListener = c -> rebuildHeader();
    private UIScheduler progressLabelAnimationScheduler;
    private FadeTransition progressLabelAnimation;

    public MuSigCreateOfferView(MuSigCreateOfferModel model, MuSigCreateOfferController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(POPUP_HEIGHT);

        progressHeaderBox = new HBox(10);
        progressHeaderBox.setAlignment(Pos.CENTER);
        progressHeaderBox.setId("wizard-progress-box");
        progressHeaderBox.setMinHeight(TOP_PANE_HEIGHT);
        progressHeaderBox.setMaxHeight(TOP_PANE_HEIGHT);
        progressHeaderBox.setPadding(new Insets(0, 20, 0, 50));

        closeButton = BisqIconButton.createIconButton("close");

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
        root.getChildren().addAll(progressHeaderBox, content, Spacer.fillVBox(), buttons);

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
        model.getChildTargets().addListener(childTargetsListener);

        rebuildHeader();

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

        model.getChildTargets().removeListener(childTargetsListener);
        model.getCurrentIndex().removeListener(currentIndexListener);
        model.getView().removeListener(viewChangeListener);

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

    private void rebuildHeader() {
        progressHeaderBox.getChildren().clear();
        progressLabelList.clear();
        progressHeaderBox.getChildren().add(Spacer.fillHBox());

        List<NavigationTarget> targets = model.getChildTargets();
        for (int i = 0; i < targets.size(); i++) {
            Label label = createAndGetProgressLabel(getLabelForTarget(targets.get(i)));
            progressLabelList.add(label);
            progressHeaderBox.getChildren().add(label);

            if (i < targets.size() - 1) {
                progressHeaderBox.getChildren().add(getHLine());
            }
        }
        progressHeaderBox.getChildren().addAll(Spacer.fillHBox(), closeButton);
        applyProgress(model.getCurrentIndex().get(), false);
    }

    private String getLabelForTarget(NavigationTarget target) {
        return switch (target) {
            case MU_SIG_CREATE_OFFER_DIRECTION_AND_MARKET -> Res.get("bisqEasy.tradeWizard.progress.directionAndMarket");
            case MU_SIG_CREATE_OFFER_AMOUNT_AND_PRICE -> Res.get("bisqEasy.tradeWizard.progress.amountAndPrice.createOffer");
            case MU_SIG_CREATE_OFFER_PAYMENT_METHODS -> Res.get("bisqEasy.tradeWizard.progress.paymentMethods");
            case MU_SIG_CREATE_OFFER_REVIEW_OFFER -> Res.get("bisqEasy.tradeWizard.progress.review");
            default -> "";
        };
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
                progressLabelAnimationScheduler = UIScheduler.run(() -> progressLabelAnimation = Transitions.fade(label, OPACITY, 1, ManagedDuration.getHalfOfDefaultDurationMillis()))
                        .after(ManagedDuration.getHalfOfDefaultDurationMillis());
            } else {
                label.setOpacity(1);
            }
        }
    }
}
