package bisq.desktop.primary.main.content.wallet.utxos;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.NonCachingController;
import bisq.wallets.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletUtxosController implements NonCachingController {
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
