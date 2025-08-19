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

package bisq.desktop.main.content.wallet.setup_wallet_wizard;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class SetupWalletWizardView extends NavigationView<VBox, SetupWalletWizardModel, SetupWalletWizardController> {
    public static final double POPUP_HEIGHT = OverlayModel.HEIGHT;
    public static final double TOP_PANE_HEIGHT = 55;
    public static final double BUTTON_HEIGHT = 32;
    public static final double BUTTON_BOTTOM = 40;
    public static final double CONTENT_HEIGHT = POPUP_HEIGHT - TOP_PANE_HEIGHT - BUTTON_HEIGHT - BUTTON_BOTTOM;
    private static final double OPACITY = 0.35;

    private final List<Label> progressLabelList = new ArrayList<>();
    private final HBox progressBarHeader, closeButtonBox;
    private final Button nextButton, backButton, progressBarHeaderCloseButton, closeButton;
    private final VBox content;
    private final ChangeListener<Number> currentIndexListener;
    private final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;

    public SetupWalletWizardView(SetupWalletWizardModel model, SetupWalletWizardController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(POPUP_HEIGHT);

        HBox progressBox = createProgressBox();

        progressBarHeaderCloseButton = BisqIconButton.createIconButton("close");
        progressBarHeader = new HBox();
        progressBarHeader.setAlignment(Pos.CENTER);
        progressBarHeader.setStyle("-fx-background-color: -bisq-dark-grey-20");
        progressBarHeader.setMinHeight(TOP_PANE_HEIGHT);
        progressBarHeader.setMaxHeight(TOP_PANE_HEIGHT);
        progressBarHeader.setPadding(new Insets(0, 20, 0, 50));
        progressBarHeader.getChildren().addAll(Spacer.fillHBox(), progressBox, Spacer.fillHBox(), progressBarHeaderCloseButton);

        closeButton = BisqIconButton.createIconButton("close");
        closeButtonBox = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonBox.setPadding(new Insets(16, 20, 0, 0));

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        backButton = new Button(Res.get("action.back"));
        backButton.setFocusTraversable(false);
        HBox buttons = new HBox(10, backButton, nextButton);
        buttons.setAlignment(Pos.CENTER);

        content = new VBox();
        content.setMinHeight(CONTENT_HEIGHT);
        content.setMaxHeight(CONTENT_HEIGHT);
        content.setAlignment(Pos.CENTER);

        VBox.setMargin(buttons, new Insets(0, 0, BUTTON_BOTTOM, 0));
        VBox.setMargin(content, new Insets(0, 40, 0, 40));
        root.getChildren().addAll(progressBarHeader, closeButtonBox, content, Spacer.fillVBox(), buttons);

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
        progressBarHeader.visibleProperty().bind(model.getShouldShowHeader());
        progressBarHeader.managedProperty().bind(model.getShouldShowHeader());
        closeButtonBox.visibleProperty().bind(model.getShouldShowHeader().not());
        closeButtonBox.managedProperty().bind(model.getShouldShowHeader().not());

        nextButton.textProperty().bind(model.getNextButtonText());
        nextButton.visibleProperty().bind(model.getNextButtonVisible());
        nextButton.managedProperty().bind(model.getNextButtonVisible());

        backButton.textProperty().bind(model.getBackButtonText());
        backButton.visibleProperty().bind(model.getBackButtonVisible());
        backButton.managedProperty().bind(model.getBackButtonVisible());

        model.getCurrentIndex().addListener(currentIndexListener);
        model.getView().addListener(viewChangeListener);

        nextButton.setOnAction(e -> controller.onNext());
        backButton.setOnAction(evt -> controller.onBack());
        progressBarHeaderCloseButton.setOnAction(e -> controller.onClose());
        closeButton.setOnAction(e -> controller.onClose());
        root.setOnKeyPressed(controller::onKeyPressed); // To handle Enter, Esc

        applyProgress(model.getCurrentIndex().get(), false);
    }

    @Override
    protected void onViewDetached() {
        progressBarHeader.visibleProperty().unbind();
        progressBarHeader.managedProperty().unbind();
        closeButtonBox.visibleProperty().unbind();
        closeButtonBox.managedProperty().unbind();

        nextButton.textProperty().unbind();
        nextButton.visibleProperty().unbind();
        nextButton.managedProperty().unbind();

        backButton.textProperty().unbind();
        backButton.visibleProperty().unbind();
        backButton.managedProperty().unbind();

        model.getCurrentIndex().removeListener(currentIndexListener);
        model.getView().removeListener(viewChangeListener);

        nextButton.setOnAction(null);
        backButton.setOnAction(null);
        progressBarHeaderCloseButton.setOnAction(null);
        closeButton.setOnAction(null);
        root.setOnKeyPressed(null);
    }

    // TODO: Generalise into OverlayWizardView
    private Region getHLine() {
        Region line = Layout.hLine();
        line.setPrefWidth(30);
        return line;
    }

    // TODO: Generalise into OverlayWizardView
    private Label createAndGetProgressLabel(String text) {
        Label label = new Label(text.toUpperCase());
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().add("bisq-text-14");
        label.setOpacity(OPACITY);
        return label;
    }

    private void applyProgress(int progressIndex, boolean delay) {
        if (progressIndex == 0) {
            return; // First step does not have a progress label.
        }

        if (progressIndex < progressLabelList.size()) {
            progressLabelList.forEach(label -> label.setOpacity(OPACITY));
            Label label = progressLabelList.get(progressIndex - 1); // -1 because first step does not have a progress label.
            if (delay) {
                UIScheduler.run(() -> Transitions.fade(label, OPACITY, 1, ManagedDuration.getHalfOfDefaultDurationMillis()))
                        .after(ManagedDuration.getHalfOfDefaultDurationMillis());
            } else {
                label.setOpacity(1);
            }
        }
    }

    private HBox createProgressBox() {
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setMinHeight(TOP_PANE_HEIGHT);
        progressBox.setMaxHeight(TOP_PANE_HEIGHT);
        progressBox.setPadding(new Insets(0, 20, 0, 5));
        progressLabelList.clear();

        Label protectWallet = createAndGetProgressLabel(Res.get("wallet.protectWallet").toUpperCase(Locale.ROOT));
        progressLabelList.add(protectWallet);
        progressBox.getChildren().add(protectWallet);

        progressBox.getChildren().add(getHLine());

        Label backupSeeds = createAndGetProgressLabel(Res.get("wallet.backupSeeds").toUpperCase(Locale.ROOT));
        progressLabelList.add(backupSeeds);
        progressBox.getChildren().add(backupSeeds);

        progressBox.getChildren().add(getHLine());

        Label verifySeeds = createAndGetProgressLabel(Res.get("wallet.verifySeeds").toUpperCase(Locale.ROOT));
        progressLabelList.add(verifySeeds);
        progressBox.getChildren().add(verifySeeds);

        return progressBox;
    }
}
