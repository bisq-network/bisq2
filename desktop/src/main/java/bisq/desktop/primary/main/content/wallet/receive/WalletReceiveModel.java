package bisq.desktop.primary.main.content.wallet.receive;

import bisq.desktop.common.view.Model;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class WalletReceiveModel implements Model {
    private final ObservableList<String> listItems = FXCollections.observableArrayList();

    public void addNewAddress(String address) {
        listItems.add(address);
    }

    public ObservableList<String> getListItems() {
        return listItems;
    }
}
