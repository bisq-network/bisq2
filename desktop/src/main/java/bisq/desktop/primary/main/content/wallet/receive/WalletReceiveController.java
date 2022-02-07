package bisq.desktop.primary.main.content.wallet.receive;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.wallets.WalletService;
import lombok.Getter;

public class WalletReceiveController implements Controller {
    private final WalletService walletService;
    private final WalletReceiveModel model;
    @Getter
    private final WalletReceiveView view;

    public WalletReceiveController(DefaultApplicationService applicationService) {
        this.walletService = applicationService.getWalletService();
        model = new WalletReceiveModel();
        view = new WalletReceiveView(model, this);
    }

    public void onGenerateNewAddressButtonClicked() {
        walletService.getNewAddress("")
                .thenAccept(newAddress -> UIThread.run(() -> model.addNewAddress(newAddress)));
    }
}
