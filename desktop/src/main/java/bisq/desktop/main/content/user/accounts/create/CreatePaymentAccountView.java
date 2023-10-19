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

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreatePaymentAccountView extends View<VBox, CreatePaymentAccountModel, CreatePaymentAccountController> {
    private final MaterialTextField accountName;
    private final MaterialTextArea accountData;
    private final Button saveButton, cancelButton;
    private final Label headLineLabel;

    public CreatePaymentAccountView(CreatePaymentAccountModel model, CreatePaymentAccountController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.CENTER);
        root.setSpacing(8);
        root.setPadding(new Insets(10, 0, 10, 0));
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);
        int width = 600;
        headLineLabel = new Label(Res.get("user.paymentAccounts.createAccount.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("user.paymentAccounts.createAccount.subtitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setMaxWidth(width);
        subtitleLabel.setMinHeight(40); // does not wrap without that...
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        accountName = new MaterialTextField(Res.get("user.paymentAccounts.createAccount.accountName"),
                Res.get("user.paymentAccounts.createAccount.accountName.prompt"));
        accountName.setPrefWidth(width);
        accountData = new MaterialTextArea(Res.get("user.paymentAccounts.createAccount.accountName"),
                Res.get("user.paymentAccounts.createAccount.accountData.prompt"));
        accountData.setPrefWidth(width);
        accountData.setFixedHeight(200);

        VBox fieldsAndButtonsVBox = new VBox(20, accountName, accountData);
        fieldsAndButtonsVBox.setPadding(new Insets(50, 0, 0, 0));
        fieldsAndButtonsVBox.setPrefWidth(width);
        fieldsAndButtonsVBox.setPrefHeight(200);
        fieldsAndButtonsVBox.setAlignment(Pos.CENTER);

        HBox.setMargin(fieldsAndButtonsVBox, new Insets(-55, 0, 0, 0));
        HBox centerHBox = new HBox(10, fieldsAndButtonsVBox);
        centerHBox.setAlignment(Pos.TOP_CENTER);

        cancelButton = new Button(Res.get("action.cancel"));
        saveButton = new Button(Res.get("action.save"));
        saveButton.setDefaultButton(true);

        HBox buttons = new HBox(20, cancelButton, saveButton);
        buttons.setAlignment(Pos.CENTER);

        VBox.setMargin(headLineLabel, new Insets(0, 0, 0, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 40, 0));
        VBox.setMargin(buttons, new Insets(60, 0, 0, 0));
        root.getChildren().addAll(
                headLineLabel,
                subtitleLabel,
                centerHBox,
                buttons
        );
    }

    @Override
    protected void onViewAttached() {
        accountData.textProperty().bindBidirectional(model.accountDataProperty());
        accountName.textProperty().bindBidirectional(model.accountNameProperty());
        saveButton.disableProperty().bind(model.saveButtonDisabledProperty());

        saveButton.setOnAction((event) -> controller.onSave());
        cancelButton.setOnAction((event) -> controller.onCancel());
    }

    @Override
    protected void onViewDetached() {
        accountData.textProperty().unbindBidirectional(model.getAccountData());
        accountName.textProperty().unbindBidirectional(model.getAccountName());
        saveButton.disableProperty().unbind();

        saveButton.setOnAction(null);
        cancelButton.setOnAction(null);
    }
}
