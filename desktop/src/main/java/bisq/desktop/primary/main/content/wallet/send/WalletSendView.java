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

package bisq.desktop.primary.main.content.wallet.send;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class WalletSendView extends View<VBox, WalletSendModel, WalletSendController> {

    private final TextField addressTextField;
    private final TextField amountTextField;
    private final PasswordField passphraseTextField;

    public WalletSendView(WalletSendModel model, WalletSendController controller) {
        super(new VBox(), model, controller);

        addressTextField = createTextField(Res.get("address") + ":");
        amountTextField = createTextField(Res.get("amount") + ":");

        passphraseTextField = new PasswordField();
        passphraseTextField.setPromptText(Res.get("passphrase") + ":");

        Button sendButton = new Button(Res.get("send"));
        sendButton.setOnMouseClicked(event -> controller.onSendButtonClicked());
        Node sendButtonInHBox = alignButtonRightInHBox(sendButton);

        root.setSpacing(20);
        root.setPadding(new Insets(20, 20, 20, 0));
        root.getChildren().addAll(addressTextField, amountTextField, sendButtonInHBox);
        root.setMaxWidth(330); // Right align Button
    }

    @Override
    protected void onViewAttached() {
        addressTextField.textProperty().bindBidirectional(model.addressProperty());
        amountTextField.textProperty().bindBidirectional(model.amountProperty());
        passphraseTextField.textProperty().bindBidirectional(model.passphraseProperty());
    }

    @Override
    protected void onViewDetached() {
        addressTextField.textProperty().unbindBidirectional(model.addressProperty());
        amountTextField.textProperty().unbindBidirectional(model.amountProperty());
        passphraseTextField.textProperty().unbindBidirectional(model.passphraseProperty());
    }

    private TextField createTextField(String promptText) {
        TextField textField = new TextField();
        textField.setPromptText(promptText);
        return textField;
    }

    private Node alignButtonRightInHBox(Button button) {
        HBox hBox = new HBox(button);
        hBox.setAlignment(Pos.BOTTOM_RIGHT);
        return hBox;
    }
}
