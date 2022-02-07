package bisq.desktop.primary.main.content.wallet.utxos;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.wallets.WalletService;
import lombok.Getter;

public class WalletUtxosController implements Controller {
    private final WalletService walletService;
    private final WalletUtxosModel model;
    @Getter
    private final WalletUtxosView view;

    public WalletUtxosController(DefaultApplicationService applicationService) {
        walletService = applicationService.getWalletService();
        model = new WalletUtxosModel();
        view = new WalletUtxosView(model, this);
    }

    @Override
    public void onViewAttached() {
        walletService.listUnspent()
                .thenAccept(utxos -> UIThread.run(() -> model.addUtxos(utxos)));
    }
}
