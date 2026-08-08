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

package bisq.desktop.main.content.wallet.receive;

import bisq.desktop.common.Transitions;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletReceiveWizardView extends NavigationView<VBox, WalletReceiveWizardModel, WalletReceiveWizardController> {
    private static final double CONTENT_WIDTH = 600;
    private static final double CONTENT_HEIGHT = 200;

    private final Button nextButton, backButton, closeIconButton;
    private final VBox contentVBox;
    private final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;

    public WalletReceiveWizardView(WalletReceiveWizardModel model, WalletReceiveWizardController controller) {
        super(new VBox(30), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        closeIconButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeIconButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label titleLabel = new Label(Res.get("wallet.receive.header"));
        titleLabel.getStyleClass().add("bisq-text-headline-2");
        HBox headlineBox = new HBox(Spacer.fillHBox(), titleLabel, Spacer.fillHBox());

        contentVBox = new VBox(30);
        contentVBox.setMinWidth(CONTENT_WIDTH);
        contentVBox.setPrefHeight(CONTENT_HEIGHT);

        HBox contentHBox = new HBox(Spacer.fillHBox(), contentVBox, Spacer.fillHBox());

        backButton = new Button(Res.get("action.back"));
        nextButton = new Button();
        nextButton.setDefaultButton(true);
        HBox buttonsBox = new HBox(20, backButton, nextButton);
        buttonsBox.setAlignment(Pos.CENTER);

        root.getChildren().setAll(closeButtonRow, headlineBox, contentHBox, buttonsBox);

        viewChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                contentVBox.getChildren().setAll(childRoot);
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
                contentVBox.getChildren().clear();
            }
        };
    }

    @Override
    protected void onViewAttached() {
        nextButton.textProperty().bind(model.getNextButtonText());
        backButton.visibleProperty().bind(model.getBackButtonVisible());
        backButton.managedProperty().bind(model.getBackButtonVisible());

        model.getView().addListener(viewChangeListener);

        nextButton.setOnAction(e -> controller.onNext());
        backButton.setOnAction(e -> controller.onBack());
        closeIconButton.setOnAction(e -> controller.onClose());
        root.setOnKeyPressed(controller::onKeyPressed);
        root.setOnMouseClicked(e -> root.requestFocus());
    }

    @Override
    protected void onViewDetached() {
        nextButton.textProperty().unbind();
        backButton.visibleProperty().unbind();
        backButton.managedProperty().unbind();

        model.getView().removeListener(viewChangeListener);

        nextButton.setOnAction(null);
        backButton.setOnAction(null);
        closeIconButton.setOnAction(null);
        root.setOnKeyPressed(null);
        root.setOnMouseClicked(null);
    }
}
