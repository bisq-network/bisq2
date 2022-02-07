package bisq.desktop.primary.main.content.wallet.send;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTextField;
import bisq.i18n.Res;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class WalletSendView extends View<VBox, WalletSendModel, WalletSendController> {
    public WalletSendView(WalletSendModel model, WalletSendController controller) {
        super(new VBox(), model, controller);

        BisqTextField addressTextField = createTextField(Res.get("address") + ":");
        addressTextField.textProperty().bindBidirectional(model.addressProperty());

        BisqTextField amountTextField = createTextField(Res.get("amount") + ":");
        amountTextField.textProperty().bindBidirectional(model.amountProperty());

        Button sendButton = new Button(Res.get("send"));
        sendButton.setOnMouseClicked(event -> controller.onSendButtonClicked());
        Node sendButtonInHbox = alignButtonRightInHBox(sendButton);

        root.setSpacing(20);
        root.setPadding(new Insets(20, 20, 20, 0));
        root.getChildren().addAll(addressTextField, amountTextField, sendButtonInHbox);
        root.setMaxWidth(330); // Right align Button
    }

    private BisqTextField createTextField(String promptText) {
        BisqTextField textField = new BisqTextField();
        textField.setPromptText(promptText);
        return textField;
    }

    private Node alignButtonRightInHBox(Button button) {
        HBox hBox = new HBox(button);
        hBox.setAlignment(Pos.BOTTOM_RIGHT);
        return hBox;
    }
}
