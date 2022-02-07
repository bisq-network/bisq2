package bisq.desktop.primary.main.content.wallet.dialog;

import bisq.desktop.common.view.Model;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

public class WalletConfigDialogModel implements Model {
    @Getter
    private final ObservableList<String> walletBackends = FXCollections.observableArrayList(
            "Bitcoin Core"
    );

    private final StringProperty hostnameProperty = new SimpleStringProperty(this, "hostname", "127.0.0.1");
    private final StringProperty portProperty = new SimpleStringProperty(this, "port", "18443");

    private final StringProperty usernameProperty = new SimpleStringProperty(this, "username", "bisq");
    private final StringProperty passwordProperty = new SimpleStringProperty(this, "password");

    private final StringProperty walletPassphraseProperty = new SimpleStringProperty(this, "wallet_passphrase");

    public StringProperty hostnameProperty() {
        return hostnameProperty;
    }

    public StringProperty portProperty() {
        return portProperty;
    }

    public StringProperty usernameProperty() {
        return usernameProperty;
    }

    public StringProperty passwordProperty() {
        return passwordProperty;
    }

    public StringProperty walletPassphraseProperty() {
        return walletPassphraseProperty;
    }
}
