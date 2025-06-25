package bisq.desktop.main.content.wallet.create_wallet.backup;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.wallet.create_wallet.backup.CreateWalletBackupModel;
import bisq.desktop.main.content.wallet.create_wallet.backup.CreateWalletBackupView;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.function.Consumer;

@Slf4j
public class CreateWalletBackupController implements Controller {
    private final CreateWalletBackupModel model;
    @Getter
    private final CreateWalletBackupView view;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;

    public CreateWalletBackupController(ServiceProvider serviceProvider,
                                         Consumer<Boolean> navigationButtonsVisibleHandler) {
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        model = new CreateWalletBackupModel();
        view = new CreateWalletBackupView(model, this);
    }

    public void init() {
        // Initialization logic for backup step
    }

    @Override
    public void onActivate() {
        // Activation logic for backup step
    }

    @Override
    public void onDeactivate() {
        navigationButtonsVisibleHandler.accept(true);
    }

    void onKeyPressedWhileShowingOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
    }
} 