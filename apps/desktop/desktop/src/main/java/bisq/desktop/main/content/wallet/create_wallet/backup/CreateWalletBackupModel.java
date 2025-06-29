package bisq.desktop.main.content.wallet.create_wallet.backup;

import bisq.desktop.common.view.Model;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CreateWalletBackupModel implements Model {
    private final StringProperty[] seedWords = new StringProperty[12];

    public CreateWalletBackupModel() {
        for (int i = 0; i < 12; i++) {
            seedWords[i] = new SimpleStringProperty("");
        }
    }

}