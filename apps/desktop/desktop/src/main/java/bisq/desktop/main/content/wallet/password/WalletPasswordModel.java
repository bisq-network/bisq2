package bisq.desktop.main.content.wallet.password;

import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Getter
public class WalletPasswordModel implements Model {
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty passwordRepeat = new SimpleStringProperty();
    private final StringProperty currentPassword = new SimpleStringProperty();
    private final BooleanProperty isCurrentPasswordVisible = new SimpleBooleanProperty();

    private final PasswordValidator passwordValidator;

    public WalletPasswordModel() {
        passwordValidator = new PasswordValidator();
    }
}
