package bisq.desktop.main.content.wallet.password;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
        //todo implement

    }


    @Override
    public void onDeactivate() {
        
    }
}
