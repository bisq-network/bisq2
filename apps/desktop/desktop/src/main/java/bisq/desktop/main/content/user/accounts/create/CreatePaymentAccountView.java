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
import java.util.Optional;

@Slf4j
public class CreatePaymentAccountView extends NavigationView<VBox, CreatePaymentAccountModel, CreatePaymentAccountController> {
    private static final double POPUP_HEIGHT = OverlayModel.HEIGHT;
    private static final double TOP_PANE_HEIGHT = 55;
    private static final double BUTTON_HEIGHT = 32;
    private static final double BUTTON_BOTTOM = 40;
    private static final double CONTENT_HEIGHT = POPUP_HEIGHT - TOP_PANE_HEIGHT - BUTTON_HEIGHT - BUTTON_BOTTOM;
    private static final double OPACITY = 0.35;

    private static final long PROGRESS_ANIMATION_DELAY = ManagedDuration.getHalfOfDefaultDurationMillis();
    private static final long PROGRESS_ANIMATION_DURATION = ManagedDuration.getHalfOfDefaultDurationMillis();


    private final List<Label> progressLabelList = new ArrayList<>();
    private final HBox progressBox;

    private final Button nextButton, backButton, closeButton, createAccountButton;

    private final VBox content;

    private final ChangeListener<Number> currentIndexListener;
    private final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;

    private Subscription showProgressBoxPin, createAccountButtonVisiblePin;

    public CreatePaymentAccountView(CreatePaymentAccountModel model, CreatePaymentAccountController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(POPUP_HEIGHT);

        Label summary = createAndGetProgressLabel(Res.get("user.paymentAccounts.createAccount.progress.summary"));
        progressLabelList.add(summary);

        progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setMinHeight(TOP_PANE_HEIGHT);
        progressBox.setMaxHeight(TOP_PANE_HEIGHT);
        progressBox.setPadding(new Insets(0, 20, 0, 50));
        progressBox.getChildren().add(summary);

        closeButton = BisqIconButton.createIconButton("close");

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setStyle("-fx-background-color: -bisq-dark-grey-20");
        headerBox.setMinHeight(TOP_PANE_HEIGHT);
        headerBox.setMaxHeight(TOP_PANE_HEIGHT);
        headerBox.setPadding(new Insets(0, 20, 0, 50));
        headerBox.getChildren().addAll(
                Spacer.fillHBox(),
                progressBox,
                Spacer.fillHBox(),
                closeButton
        );

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        createAccountButton = new Button(Res.get("user.paymentAccounts.createAccount.createAccount"));
        createAccountButton.setDefaultButton(true);

        backButton = new Button(Res.get("action.back"));
        backButton.setFocusTraversable(false);

        HBox buttons = new HBox(10, backButton, nextButton, createAccountButton);
        buttons.setAlignment(Pos.CENTER);

        content = new VBox();
        content.setMinHeight(CONTENT_HEIGHT);
        content.setMaxHeight(CONTENT_HEIGHT);
        content.setAlignment(Pos.CENTER);

        viewChangeListener =
                (observable, oldValue, newValue) ->
                        Optional.ofNullable(newValue).ifPresentOrElse(
                                view -> {
                                    Region childRoot = view.getRoot();
                                    childRoot.setMinHeight(CONTENT_HEIGHT);
                                    childRoot.setMaxHeight(CONTENT_HEIGHT);
                                    content.getChildren().setAll(childRoot);

                                    Optional.ofNullable(oldValue).ifPresentOrElse(
                                            oldView -> {
                                                if (model.isAnimateRightOut()) {
                                                    Transitions.transitRightOut(childRoot, oldView.getRoot());
                                                } else {
                                                    Transitions.transitLeftOut(childRoot, oldView.getRoot());
                                                }
                                            },
                                            () -> Transitions.fadeIn(childRoot)
                                    );
                                },
                                () -> content.getChildren().clear()
                        );


        currentIndexListener = (observable, oldValue, newValue) ->
                applyProgress(newValue.intValue(), true);

        VBox.setMargin(buttons, new Insets(0, 0, BUTTON_BOTTOM, 0));
        VBox.setMargin(content, new Insets(0, 40, 0, 40));
        root.getChildren().addAll(headerBox, content, Spacer.fillVBox(), buttons);
    }

    @Override
    protected void onViewAttached() {
        buildProgressBar();
        setupBindings();
        setupEventHandlers();
        applyProgress(model.getCurrentIndex().get(), false);
    }

    @Override
    protected void onViewDetached() {
        cleanupBindings();
        cleanupEventHandlers();
    }

    private void buildProgressBar() {
        if (model.isOptionsVisible()) {
            Label options = createAndGetProgressLabel(Res.get("user.paymentAccounts.createAccount.progress.options"));
            progressLabelList.addFirst(options);
            progressBox.getChildren().addFirst(getHLine());
            progressBox.getChildren().addFirst(options);
        }

        Label accountData =
                createAndGetProgressLabel(Res.get("user.paymentAccounts.createAccount.progress.accountData"));
        progressLabelList.addFirst(accountData);
        progressBox.getChildren().addFirst(getHLine());
        progressBox.getChildren().addFirst(accountData);

        Label paymentMethod =
                createAndGetProgressLabel(Res.get("user.paymentAccounts.createAccount.progress.paymentMethod"));
        progressLabelList.addFirst(paymentMethod);
        progressBox.getChildren().addFirst(getHLine());
        progressBox.getChildren().addFirst(paymentMethod);
    }

    private void setupBindings() {
        setupButtonBindings();

        setupCreateAccountButtonBinding();
        setupProgressBoxBinding();

        model.getCurrentIndex().addListener(currentIndexListener);
        model.getView().addListener(viewChangeListener);
    }

    private void setupButtonBindings() {
        nextButton.textProperty().bind(model.getNextButtonText());
        nextButton.visibleProperty().bind(model.getNextButtonVisible());
        nextButton.managedProperty().bind(model.getNextButtonVisible());

        backButton.textProperty().bind(model.getBackButtonText());
        backButton.visibleProperty().bind(model.getBackButtonVisible());
        backButton.managedProperty().bind(model.getBackButtonVisible());

        closeButton.visibleProperty().bind(model.getCloseButtonVisible());

        createAccountButton.visibleProperty().bind(model.getCreateAccountButtonVisible());
        createAccountButton.managedProperty().bind(model.getCreateAccountButtonVisible());
    }

    private void setupCreateAccountButtonBinding() {
        createAccountButtonVisiblePin =
                EasyBind.subscribe(model.getCreateAccountButtonVisible(), this::handleCreateButtonVisibilityChange);
    }

    private void handleCreateButtonVisibilityChange(boolean createButtonVisible) {
        if (createButtonVisible) {
            backButton.prefWidthProperty().bind(createAccountButton.widthProperty());
        } else {
            backButton.prefWidthProperty().unbind();
            backButton.setPrefWidth(Region.USE_COMPUTED_SIZE);
        }
    }

    private void setupProgressBoxBinding() {
        showProgressBoxPin = EasyBind.subscribe(model.getShowProgressBox(), showProgressBox -> {
            progressBox.setVisible(showProgressBox);
            progressBox.setManaged(showProgressBox);
        });
    }

    private void setupEventHandlers() {
        nextButton.setOnAction(e -> controller.onNext());
        backButton.setOnAction(evt -> controller.onBack());
        closeButton.setOnAction(e -> controller.onClose());
        createAccountButton.setOnAction(e -> controller.onCreateAccount());

        root.setOnKeyPressed(controller::onKeyPressed);
    }

    private void cleanupBindings() {
        nextButton.textProperty().unbind();
        nextButton.visibleProperty().unbind();
        nextButton.managedProperty().unbind();

        backButton.textProperty().unbind();
        backButton.visibleProperty().unbind();
        backButton.managedProperty().unbind();
        backButton.prefWidthProperty().unbind();

        closeButton.visibleProperty().unbind();

        createAccountButton.visibleProperty().unbind();
        createAccountButton.managedProperty().unbind();

        model.getCurrentIndex().removeListener(currentIndexListener);
        model.getView().removeListener(viewChangeListener);

        Optional.ofNullable(showProgressBoxPin).ifPresent(pin -> {
            pin.unsubscribe();
            showProgressBoxPin = null;
        });
        Optional.ofNullable(createAccountButtonVisiblePin).ifPresent(pin -> {
            pin.unsubscribe();
            createAccountButtonVisiblePin = null;
        });
    }

    private void cleanupEventHandlers() {
        nextButton.setOnAction(null);
        backButton.setOnAction(null);
        closeButton.setOnAction(null);
        createAccountButton.setOnAction(null);
        root.setOnKeyPressed(null);
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
        if (progressIndex < 0 || progressIndex >= progressLabelList.size()) {
            log.warn("Invalid progress index: {} for list size: {}", progressIndex, progressLabelList.size());
            return;
        }
        progressLabelList.forEach(label -> label.setOpacity(OPACITY));

        Optional.of(progressLabelList.get(progressIndex)).ifPresent(currentLabel -> {
            if (delay) {
                UIScheduler.run(() ->
                                Transitions.fade(currentLabel, OPACITY, 1,
                                        PROGRESS_ANIMATION_DURATION))
                        .after(PROGRESS_ANIMATION_DELAY);
            } else {
                currentLabel.setOpacity(1);
            }
        });
    }
}