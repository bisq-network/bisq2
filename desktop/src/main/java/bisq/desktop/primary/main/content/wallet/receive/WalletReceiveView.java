package bisq.desktop.primary.main.content.wallet.receive;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class WalletReceiveView extends View<VBox, WalletReceiveModel, WalletReceiveController> {
    public WalletReceiveView(WalletReceiveModel model, WalletReceiveController controller) {
        super(new VBox(), model, controller);

        var generateNewAddressButton = new Button(Res.get("wallet.receive.generateNewAddress"));
        generateNewAddressButton.setOnMouseClicked(event -> controller.onGenerateNewAddressButtonClicked());

        ListView<String> listView = new ListView<>();
        listView.setCellFactory(param -> new WalletReceiveListCell());
        listView.setItems(model.getListItems());

        root.getChildren().addAll(generateNewAddressButton, listView);
    }
}
