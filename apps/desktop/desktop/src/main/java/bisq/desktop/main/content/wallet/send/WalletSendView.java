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

package bisq.desktop.main.content.wallet.send;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class WalletSendView extends View<VBox, WalletSendModel, WalletSendController> {
    private final MaterialTextField address, amount;
    private final MaterialPasswordField password;
    private final Button sendButton, closeIconButton, closeButton;
    private final VBox formBox, sentBox;
    private final Label txIdLabel;
    private final BisqMenuItem copyTxIdButton;
    private Subscription sendInProgressPin, sentTxIdPin;

    public WalletSendView(WalletSendModel model, WalletSendController controller) {
        super(new VBox(30), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH - 20);
        root.setPrefHeight(OverlayModel.HEIGHT - 20);
        root.setAlignment(Pos.TOP_LEFT);

        closeIconButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeIconButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label titleLabel = new Label(Res.get("wallet.send.header"));
        titleLabel.getStyleClass().add("bisq-text-headline-2");
        HBox headlineBox = new HBox(Spacer.fillHBox(), titleLabel, Spacer.fillHBox());

        address = new MaterialTextField(Res.get("wallet.send.address"), null, null);
        address.setValidator(model.getAddressValidator());
        amount = new MaterialTextField(Res.get("wallet.send.amount"), null, null);
        amount.setValidator(model.getAmountValidator());
        password = new MaterialPasswordField(Res.get("wallet.send.password"), null, null);

        formBox = new VBox(20);
        formBox.setPadding(new Insets(0, 70, 0, 70));
        formBox.getChildren().addAll(address, amount, password);

        Label sentLabel = new Label(Res.get("wallet.send.sent.headline"));
        sentLabel.getStyleClass().add("bisq-text-headline-3");
        Label txIdDescriptionLabel = new Label(Res.get("wallet.send.sent.txId"));
        txIdDescriptionLabel.getStyleClass().add("bisq-text-3");
        txIdLabel = new Label();
        txIdLabel.getStyleClass().add("bisq-text-1");
        copyTxIdButton = new BisqMenuItem("copy-green", "copy-white");
        copyTxIdButton.setTooltip(Res.get("wallet.send.sent.copyTxId"));
        HBox txIdBox = new HBox(10, txIdLabel, copyTxIdButton);
        txIdBox.setAlignment(Pos.CENTER_LEFT);
        sentBox = new VBox(10, sentLabel, txIdDescriptionLabel, txIdBox);
        sentBox.setPadding(new Insets(0, 70, 0, 70));

        closeButton = new Button(Res.get("action.close"));
        sendButton = new Button(Res.get("wallet.send.sendBtc"));
        sendButton.setDefaultButton(true);
        HBox buttonsBox = new HBox(20, closeButton, sendButton);
        buttonsBox.setAlignment(Pos.CENTER);

        root.getChildren().setAll(closeButtonRow, headlineBox, formBox, sentBox, buttonsBox);
    }

    @Override
    protected void onViewAttached() {
        address.textProperty().bindBidirectional(model.getAddress());
        amount.textProperty().bindBidirectional(model.getAmount());
        password.textProperty().bindBidirectional(model.getPassword());
        password.visibleProperty().bind(model.getIsPasswordVisible());
        password.managedProperty().bind(model.getIsPasswordVisible());
        sendButton.disableProperty().bind(model.getSendInProgress());
        txIdLabel.textProperty().bind(model.getSentTxId());

        sendInProgressPin = EasyBind.subscribe(model.getSendInProgress(), inProgress -> {
            sendButton.setText(Res.get(inProgress ? "wallet.send.sending" : "wallet.send.sendBtc"));
            address.setEditable(!inProgress);
            amount.setEditable(!inProgress);
            password.setEditable(!inProgress);
        });
        sentTxIdPin = EasyBind.subscribe(model.getSentTxId(), txId -> {
            boolean sent = txId != null;
            formBox.setVisible(!sent);
            formBox.setManaged(!sent);
            sendButton.setVisible(!sent);
            sendButton.setManaged(!sent);
            sentBox.setVisible(sent);
            sentBox.setManaged(sent);
        });

        sendButton.setOnAction(e -> {
            address.validate();
            amount.validate();
            controller.onSend();
        });
        copyTxIdButton.setOnAction(e -> controller.onCopyTxId());
        closeIconButton.setOnAction(e -> controller.onClose());
        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        address.textProperty().unbindBidirectional(model.getAddress());
        amount.textProperty().unbindBidirectional(model.getAmount());
        password.textProperty().unbindBidirectional(model.getPassword());
        password.visibleProperty().unbind();
        password.managedProperty().unbind();
        sendButton.disableProperty().unbind();
        txIdLabel.textProperty().unbind();

        sendInProgressPin.unsubscribe();
        sentTxIdPin.unsubscribe();

        sendButton.setOnAction(null);
        copyTxIdButton.setOnAction(null);
        closeIconButton.setOnAction(null);
        closeButton.setOnAction(null);

        address.resetValidation();
        amount.resetValidation();
    }
}
