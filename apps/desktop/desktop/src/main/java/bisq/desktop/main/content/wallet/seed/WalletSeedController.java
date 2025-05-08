package bisq.desktop.main.content.wallet.seed;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;


@Getter
@Slf4j
public class WalletSeedController implements Controller {

    private final WalletSeedModel model;
    @Getter
    private final WalletSeedView view;
    private final WalletService walletService;

    private Subscription currentPasswordPin;


    public WalletSeedController(ServiceProvider serviceProvider) {
        model = new WalletSeedModel();
        view = new WalletSeedView(model, this);
        walletService = serviceProvider.getWalletService().orElseThrow();
    }


    @Override
    public void onActivate() {
        currentPasswordPin = EasyBind.subscribe(model.getCurrentPassword(), password -> {
            //todo implement
            UIThread.run(() -> {
                model.getShowSeedButtonDisable().set(false);
            });
        });
        walletService.isWalletEncrypted().thenAccept(isEncrypted -> {
            UIThread.run(() -> {
                model.getIsCurrentPasswordVisible().set(isEncrypted);
                model.getShowSeedButtonDisable().set(isEncrypted);
            });
        });
    }

    public void onShowSeed() {
        //todo implement
//        walletService.getSeed().whenComplete((seed, throwable) -> {
//            if (throwable == null) {
//                model.getWalletSeed().set(seed);
//            }
//        });

    }

    public void onRestore() {
        //todo implement
        String seed = model.getRestoreSeed().get();

    }

    public void onSeedValidator() {
        //todo implement
        model.getRestoreButtonDisable().set(false);
    }

    @Override
    public void onDeactivate() {
        currentPasswordPin.unsubscribe();
        model.getWalletSeed().set(null);
        model.getRestoreSeed().set(null);
        model.getRestoreButtonDisable().set(true);
    }
}
