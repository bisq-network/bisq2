package bisq.desktop.primary.main.content.wallet.send;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.wallets.WalletService;
import lombok.Getter;

public class WalletSendController implements Controller {
    private final WalletService walletService;
    private final WalletSendModel model;
    @Getter
    private final WalletSendView view;

    public WalletSendController(DefaultApplicationService applicationService) {
        walletService = applicationService.getWalletService();
        model = new WalletSendModel();
        view = new WalletSendView(model, this);
    }

    public void onSendButtonClicked() {
        walletService.sendToAddress(model.getAddress(), Double.parseDouble(model.getAmount()));
    }
}
