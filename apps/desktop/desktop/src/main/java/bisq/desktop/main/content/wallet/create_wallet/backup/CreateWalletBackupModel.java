package bisq.desktop.main.content.wallet.create_wallet.backup;

import bisq.desktop.common.view.Model;
import bisq.desktop.main.content.wallet.create_wallet.SeedState;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CreateWalletBackupModel implements Model {
    private final int SEED_WORD_COUNT = 12;
    private final StringProperty[] seedWords = new StringProperty[SEED_WORD_COUNT];
    private final ObjectProperty<SeedState> seedState = new SimpleObjectProperty<>(SeedState.LOADING);

    public CreateWalletBackupModel() {
        for (int i = 0; i < SEED_WORD_COUNT; i++) {
            seedWords[i] = new SimpleStringProperty("");
        }
    }

}

