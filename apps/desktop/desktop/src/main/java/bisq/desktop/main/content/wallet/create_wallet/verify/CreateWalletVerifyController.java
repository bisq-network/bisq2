package bisq.desktop.main.content.wallet.create_wallet.verify;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.wallets.core.WalletService;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class CreateWalletVerifyController implements Controller {
    private final CreateWalletVerifyModel model;
    @Getter
    private final CreateWalletVerifyView view;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final Runnable onBackHandler;
    private final WalletService walletService;

    public CreateWalletVerifyController(ServiceProvider serviceProvider,
                                        Consumer<Boolean> navigationButtonsVisibleHandler,
                                        Consumer<NavigationTarget> closeAndNavigateToHandler,
                                        Runnable onBackHandler) {
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        this.onBackHandler = onBackHandler;

        model = new CreateWalletVerifyModel();
        view = new CreateWalletVerifyView(model, this);

        walletService = serviceProvider.getWalletService().orElseThrow();
    }

    @Override
    public void onActivate() {
        navigationButtonsVisibleHandler.accept(false);
        model.reset();
        loadSeedWordsAsync(walletService);
    }

    @Override
    public void onDeactivate() {
    }

    public void onAnswerSelected(int idx) {
        model.getSelectedAnswerIndex().set(idx);
    }

    public void onNextWordSelected() {
        int qIdx = model.getCurrentQuestionIndex().get();
        int selectedIdx = model.getSelectedAnswerIndex().get();
        // Defensive: do nothing if no answer selected
        if (selectedIdx == -1) {
            return;
        }
        int correctIdx = model.getCorrectAnswerIndices().get(qIdx);
        if (selectedIdx == correctIdx) {
            if (qIdx == CreateWalletVerifyModel.QUESTIONS_COUNT - 1) { // Last Q
                // Use settings if needed
                //walletService.setIsWalletBackedup(true);
                model.getCurrentScreenState().set(CreateWalletVerifyModel.ScreenState.SUCCESS);
                navigationButtonsVisibleHandler.accept(false);
            } else {
                model.getCurrentQuestionIndex().set(qIdx + 1);
            }
        } else {
            new Popup().warning(Res.get("wallet.verifySeeds.wrongWord.description"))
                    .owner(getPopupOwner())
                    .headline(Res.get("wallet.verifySeeds.wrongWord.title"))
                    .hideCloseButton()
                    .onAction(onBackHandler)
                    .show();
        }
    }

    public CreateWalletVerifyModel getModel() {
        return model;
    }

    public void setSeedWords(List<String> seedWords) {
        if (seedWords.size() != 12) {
            throw new IllegalArgumentException("Expected 12 seed words, got " + seedWords.size());
        }
        for (int i = 0; i < 12; i++) {
            model.getSeedWords()[i].set(seedWords.get(i));
        }
        model.setupQuestions(seedWords);
    }

    private void loadSeedWordsAsync(WalletService walletService) {
        walletService.getSeedWords()
                .thenAccept(seedWords ->
                        UIThread.run(() -> {
                            setSeedWords(seedWords);
                        }))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

                    log.error("loadSeedWordsAsync :: Failed to load seed words", cause);

                    new Popup().invalid("wallet.backupSeeds.error.failedToLoad")
                            .owner((Region) view.getRoot().getParent().getParent())
                            .show();

                    return null;
                });
    }

    void onCreateWallet() {
        closeAndNavigateToHandler.accept(NavigationTarget.WALLET);
    }

    private Region getPopupOwner() {
        return (Region) view.getRoot().getParent().getParent();
    }

} 