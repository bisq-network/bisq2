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

package bisq.desktop.main.content.user.accounts.create;

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

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CreatePaymentAccountView extends NavigationView<VBox, CreatePaymentAccountModel, CreatePaymentAccountController> {
    private static final double POPUP_HEIGHT = OverlayModel.HEIGHT;
    private static final double TOP_PANE_HEIGHT = 55;
    private static final double BUTTON_HEIGHT = 32;
    private static final double BUTTON_BOTTOM = 40;
    private static final double CONTENT_HEIGHT = POPUP_HEIGHT - TOP_PANE_HEIGHT - BUTTON_HEIGHT - BUTTON_BOTTOM;
    private static final double OPACITY = 0.35;

    private final List<Label> progressLabelList = new ArrayList<>();
    private final Button nextButton, backButton, closeButton, createAccountButton;
    private final VBox content;
    private final ChangeListener<Number> currentIndexListener;
    private final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;
    private final Region optionsHLine;
    private final Label options;
    private UIScheduler progressLabelAnimationScheduler;
    private FadeTransition progressLabelAnimation;

    public CreatePaymentAccountView(CreatePaymentAccountModel model, CreatePaymentAccountController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(POPUP_HEIGHT);

        Label paymentMethod = createAndGetProgressLabel(Res.get("user.paymentAccounts.createAccount.progress.paymentMethod"));
        progressLabelList.add(paymentMethod);

        Label accountData = createAndGetProgressLabel(Res.get("user.paymentAccounts.createAccount.progress.accountData"));
        progressLabelList.add(accountData);

        options = createAndGetProgressLabel(Res.get("user.paymentAccounts.createAccount.progress.options"));
        progressLabelList.add(options);
        optionsHLine = getHLine();

        Label summary = createAndGetProgressLabel(Res.get("user.paymentAccounts.createAccount.progress.summary"));
        progressLabelList.add(summary);

        HBox progressBox = new HBox(10,
                paymentMethod, getHLine(),
                accountData, getHLine(),
                options, optionsHLine,
                summary);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setMinHeight(TOP_PANE_HEIGHT);
        progressBox.setMaxHeight(TOP_PANE_HEIGHT);
        progressBox.setPadding(new Insets(0, 20, 0, 50));

        closeButton = BisqIconButton.createIconButton("close");
        closeButton.setFocusTraversable(false);

        HBox progressItemsBox = new HBox();
        progressItemsBox.setAlignment(Pos.CENTER);
        progressItemsBox.setId("wizard-progress-box");
        progressItemsBox.setMinHeight(TOP_PANE_HEIGHT);
        progressItemsBox.setMaxHeight(TOP_PANE_HEIGHT);
        progressItemsBox.setPadding(new Insets(0, 20, 0, 50));
        progressItemsBox.getChildren().addAll(
                Spacer.fillHBox(),
                progressBox,
                Spacer.fillHBox(),
                closeButton
        );

        backButton = new Button(Res.get("action.back"));
        backButton.setFocusTraversable(false);

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        createAccountButton = new Button(Res.get("user.paymentAccounts.createAccount.createAccount"));
        createAccountButton.setDefaultButton(true);

        HBox buttons = new HBox(10, backButton, nextButton, createAccountButton);
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
        boolean optionsVisible = model.isOptionsVisible();
        options.setVisible(optionsVisible);
        options.setManaged(optionsVisible);
        optionsHLine.setVisible(optionsVisible);
        optionsHLine.setManaged(optionsVisible);
        if (!optionsVisible) {
            progressLabelList.remove(options);
        }

        nextButton.visibleProperty().bind(model.getNextButtonVisible());
        nextButton.managedProperty().bind(model.getNextButtonVisible());
        nextButton.disableProperty().bind(model.getNextButtonDisabled());

        backButton.visibleProperty().bind(model.getBackButtonVisible());
        backButton.managedProperty().bind(model.getBackButtonVisible());

        createAccountButton.visibleProperty().bind(model.getCreateAccountButtonVisible());
        createAccountButton.managedProperty().bind(model.getCreateAccountButtonVisible());

        model.getCurrentIndex().addListener(currentIndexListener);
        model.getView().addListener(viewChangeListener);

        nextButton.setOnAction(e -> controller.onNext());
        backButton.setOnAction(evt -> controller.onBack());
        closeButton.setOnAction(e -> controller.onClose());
        createAccountButton.setOnAction(e -> controller.onCreateAccount());

        root.setOnKeyPressed(controller::onKeyPressed);

        applyProgress(model.getCurrentIndex().get(), false);
    }

    @Override
    protected void onViewDetached() {
        nextButton.textProperty().unbind();
        nextButton.visibleProperty().unbind();
        nextButton.managedProperty().unbind();
        nextButton.disableProperty().unbind();

        backButton.visibleProperty().unbind();
        backButton.managedProperty().unbind();

        createAccountButton.visibleProperty().unbind();
        createAccountButton.managedProperty().unbind();

        model.getCurrentIndex().removeListener(currentIndexListener);
        model.getView().removeListener(viewChangeListener);

        nextButton.setOnAction(null);
        backButton.setOnAction(null);
        closeButton.setOnAction(null);
        createAccountButton.setOnAction(null);
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