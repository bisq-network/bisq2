package bisq.desktop.primary.main.content.wallet.transactions;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.wallets.WalletService;
import lombok.Getter;

public class WalletTransactionsController implements Controller {
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
