package bisq.desktop.primary.main.content.wallet.receive;

import bisq.desktop.common.utils.ClipboardUtil;
import bisq.i18n.Res;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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
            copyToClipboardButton.setOnMouseClicked(event -> ClipboardUtil.copyToClipboard(item));
            setGraphic(root);
        }
    }
}
