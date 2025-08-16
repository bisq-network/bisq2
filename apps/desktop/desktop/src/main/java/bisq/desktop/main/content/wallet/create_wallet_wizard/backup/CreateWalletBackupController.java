package bisq.desktop.main.content.wallet.create_wallet_wizard.backup;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.wallet.create_wallet_wizard.SeedState;
import bisq.i18n.Res;
import bisq.wallet.WalletService;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

import java.util.List;

@Slf4j
public class CreateWalletBackupController implements Controller {
    private final CreateWalletBackupModel model;
    @Getter
    private final CreateWalletBackupView view;
    private final Runnable onBackHandler;
    private final WalletService walletService;

    public CreateWalletBackupController(ServiceProvider serviceProvider,
                                        Consumer<Boolean> navigationButtonsVisibleHandler,
                                        Runnable onBackHandler) {
        this.onBackHandler = onBackHandler;
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
        if (seedWords.size() != model.getSEED_WORD_COUNT()) {
            throw new IllegalArgumentException(
                    "Expected " + model.getSEED_WORD_COUNT() + " seed words, but got " + seedWords.size()
            );
        }
        for (int i = 0; i < model.getSEED_WORD_COUNT(); i++) {
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
                        }))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

                    log.error("Failed to load seed words", cause);
                    UIThread.run(() -> model.getSeedState().set(SeedState.ERROR));

                    new Popup().error(Res.get("wallet.backupSeeds.error.failedToLoad"))
                            .owner(getPopupOwner())
                            .onAction(onBackHandler)
                            .onClose(onBackHandler)
                            .show();

                    return null;
                });
    }

    private Region getPopupOwner() {
        return (Region) view.getRoot().getParent().getParent();
    }


}