package bisq.desktop.primary.main.content.wallet.receive;

import bisq.i18n.Res;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;

public class WalletReceiveListCell extends ListCell<String> {
    private final HBox root;
    private final Label addressLabel;
    private final Button copyToClipboardButton;

    public WalletReceiveListCell() {
        root = new HBox();
        addressLabel = new Label();
        copyToClipboardButton = new Button(Res.get("wallet.receive.copy"));
        root.getChildren().addAll(addressLabel, copyToClipboardButton);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            addressLabel.setText(item);
            copyToClipboardButton.setOnMouseClicked(event -> copyToClipboard(item));
            setGraphic(root);
        }
    }

    private void copyToClipboard(String contentToCopy) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(contentToCopy);
        clipboard.setContent(content);
    }
}
