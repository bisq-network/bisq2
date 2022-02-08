package bisq.desktop.primary.main.content.wallet.transactions;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.NonCachingController;
import bisq.wallets.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletTransactionsController implements NonCachingController {
    private final WalletService walletService;
    private final WalletTransactionsModel model;
    @Getter
    private final WalletTransactionsView view;

    public WalletTransactionsController(DefaultApplicationService applicationService) {
        walletService = applicationService.getWalletService();
        model = new WalletTransactionsModel();
        view = new WalletTransactionsView(model, this);
    }

    @Override
    public void onViewAttached() {
        walletService.listTransactions()
                .thenAccept(txs -> UIThread.run(() -> model.addTransactions(txs)));
    }
}
