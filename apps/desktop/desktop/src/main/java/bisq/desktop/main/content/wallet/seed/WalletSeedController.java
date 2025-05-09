package bisq.desktop.main.content.wallet.seed;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;


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
            boolean isValid = password != null && !password.isEmpty();
            UIThread.run(() -> {
                model.getShowSeedButtonDisable().set(!isValid);
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
        walletService.getSeed(Optional.ofNullable(model.getCurrentPassword().get())).whenComplete((seed, throwable) -> {
            if (throwable == null) {
                model.getWalletSeed().set(seed);
            }
        });

    }

    public void onRestore() {
        walletService.restoreFromSeed(model.getRestoreSeed().get()).thenAccept(success -> {
            UIThread.run(() -> {
                model.getRestoreSeed().set("");
            });
            log.info("Wallets restored with seed words");
            new Popup().feedback(Res.get("wallet.seed.restore.success")).show();
        }).exceptionally(throwable -> {
            log.error("Failed to restore wallet", throwable);
            new Popup().error(Res.get("wallet.seed.restore.error", Res.get("wallet.common.errorMessageInline", throwable)))
                    .show();
            return null;
        });

    }

    public void onSeedValidator() {
        String seed = model.getRestoreSeed().get();
        boolean isValid = seed != null && !seed.isEmpty() && validateSeedFormat(seed);
        model.getRestoreButtonDisable().set(!isValid);
    }

    private boolean validateSeedFormat(String seed) {
        // Check if seed has correct number of words (typically 12, 18 or 24)
        String[] words = seed.trim().split("\\s+");
        return words.length == 12 || words.length == 18 || words.length == 24;
    }

    @Override
    public void onDeactivate() {
        if (currentPasswordPin != null) {
            currentPasswordPin.unsubscribe();
        }
        model.getWalletSeed().set(null);
        model.getRestoreSeed().set(null);
        model.getRestoreButtonDisable().set(true);
    }
}
