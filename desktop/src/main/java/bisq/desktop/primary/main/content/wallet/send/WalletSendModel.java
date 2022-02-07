package bisq.desktop.primary.main.content.wallet.send;

import bisq.desktop.common.view.Model;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class WalletSendModel implements Model {
    private final StringProperty addressProperty = new SimpleStringProperty(this, "address");
    private final StringProperty amountProperty = new SimpleStringProperty(this, "amount");

    public StringProperty addressProperty() {
        return addressProperty;
    }

    public StringProperty amountProperty() {
        return amountProperty;
    }

    public String getAddress() {
        return addressProperty.get();
    }

    public String getAmount() {
        return amountProperty.get();
    }
}
