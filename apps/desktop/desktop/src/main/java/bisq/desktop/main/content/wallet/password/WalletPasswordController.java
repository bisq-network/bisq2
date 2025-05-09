package bisq.desktop.main.content.wallet.password;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Getter
@Slf4j
public class WalletPasswordController implements Controller {

    private final WalletPasswordModel model;
    @Getter
    private final WalletPasswordView view;

    private final WalletService walletService;

    public WalletPasswordController(ServiceProvider serviceProvider) {
        model = new WalletPasswordModel();
        view = new WalletPasswordView(model, this);
        walletService = serviceProvider.getWalletService().orElseThrow();
    }


    @Override
    public void onActivate() {
        walletService.isWalletEncrypted().thenAccept(isEncrypted -> {
            UIThread.run(() -> model.getIsCurrentPasswordVisible().set(isEncrypted));
        });
    }

    public void onApplyPassword() {
        String password = model.getPassword().get();
        walletService.encryptWallet(password, Optional.ofNullable(model.getCurrentPassword().get()))
                .thenAccept(success -> {
                    UIThread.run(() -> {
                        model.getIsCurrentPasswordVisible().set(true);
                    });
                    new Popup()
                            .feedback(Res.get("wallet.password.walletEncrypted"))
                            .show();
                })
                .exceptionally(e -> {
                    log.error("Failed to encrypt wallet", e);
                    new Popup()
                            .warning(Res.get("wallet.password.walletEncryptionFailed"))
                            .show();
                    return null;
                });
    }


    @Override
    public void onDeactivate() {
        
    }
}
