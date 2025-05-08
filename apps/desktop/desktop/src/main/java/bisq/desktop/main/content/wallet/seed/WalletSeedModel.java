package bisq.desktop.main.content.wallet.seed;

import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Getter
public class WalletSeedModel implements Model {
    private final StringProperty walletSeed = new SimpleStringProperty();
    private final BooleanProperty isCurrentPasswordVisible = new SimpleBooleanProperty();
    private final BooleanProperty showSeedButtonDisable = new SimpleBooleanProperty();
    private final StringProperty currentPassword = new SimpleStringProperty();

    private final StringProperty restoreSeed = new SimpleStringProperty();
    private final BooleanProperty restoreButtonDisable = new SimpleBooleanProperty(true);


    public WalletSeedModel() {
    }
}
