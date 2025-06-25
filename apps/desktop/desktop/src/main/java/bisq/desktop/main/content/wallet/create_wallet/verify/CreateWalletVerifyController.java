package bisq.desktop.main.content.wallet.create_wallet.verify;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.wallet.create_wallet.verify.CreateWalletVerifyModel;
import bisq.desktop.main.content.wallet.create_wallet.verify.CreateWalletVerifyView;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.function.Consumer;

@Slf4j
public class CreateWalletVerifyController implements Controller {
    private final CreateWalletVerifyModel model;
    @Getter
    private final CreateWalletVerifyView view;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;

    public CreateWalletVerifyController(ServiceProvider serviceProvider,
                                         Consumer<Boolean> navigationButtonsVisibleHandler) {
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        model = new CreateWalletVerifyModel();
        view = new CreateWalletVerifyView(model, this);
    }

    public void init() {
        // Initialization logic for verify step
    }

    @Override
    public void onActivate() {
        // Activation logic for verify step
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