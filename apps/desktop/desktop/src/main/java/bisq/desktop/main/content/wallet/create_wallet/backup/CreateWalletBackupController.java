package bisq.desktop.main.content.wallet.create_wallet.backup;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.wallet.create_wallet.SeedState;
import bisq.wallets.core.WalletService;
import javafx.beans.value.ChangeListener;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;

import java.util.List;

@Slf4j
public class CreateWalletBackupController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(CreateWalletBackupController.class);
    private final CreateWalletBackupModel model;
    @Getter
    private final CreateWalletBackupView view;
    WalletService walletService;

    public CreateWalletBackupController(ServiceProvider serviceProvider,
        Consumer<Boolean> navigationButtonsVisibleHandler) {
        model = new CreateWalletBackupModel();
        view = new CreateWalletBackupView(model, this, navigationButtonsVisibleHandler);

        walletService = serviceProvider.getWalletService().orElseThrow();
    }

    @Override
    public void onActivate() {
        loadSeedWordsAsync(walletService);
    }

    @Override
    public void onDeactivate() {

    }

    public void onRetrySeed() {
        loadSeedWordsAsync(walletService);
    }

    private void setSeedWords(List<String> seedWords) {
        for (int i = 0; i < 12; i++) {
            model.getSeedWords()[i].set(seedWords.get(i));
        }
    }

    private void loadSeedWordsAsync(WalletService walletService) {
        model.getSeedState().set(SeedState.LOADING);
        walletService.getSeedWords()
                .thenAccept(seedWords ->
                        UIThread.run(() -> {
                            setSeedWords(seedWords);
                            model.getSeedState().set(SeedState.SUCCESS);
                            log.error("loadSeedWordsAsync :: Backup :: Loaded seed words");
                        }))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

                    log.error("loadSeedWordsAsync :: Failed to load seed words", cause);
                    UIThread.run(() -> model.getSeedState().set(SeedState.ERROR));

                    new Popup().invalid("Error loading seed. Try again")
                            .owner((Region) view.getRoot().getParent().getParent())
                            .show();

                    return null;
                });
    }

}