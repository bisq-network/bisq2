package bisq.desktop.main.content.wallet.create_wallet.verify;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.wallet.create_wallet.SeedState;
import bisq.wallets.core.WalletService;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
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
    WalletService walletService;

    public CreateWalletVerifyController(ServiceProvider serviceProvider,
                                         Consumer<Boolean> navigationButtonsVisibleHandler) {
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        model = new CreateWalletVerifyModel();
        view = new CreateWalletVerifyView(model, this);

        walletService = serviceProvider.getWalletService().orElseThrow();
    }

    @Override
    public void onActivate() {
        navigationButtonsVisibleHandler.accept(false);
        model.getCurrentQuestionIndex().set(0);
        model.getSelectedAnswerIndex().set(-1);
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
            if (qIdx == 5) {
                model.getCurrentScreenState().set(CreateWalletVerifyModel.ScreenState.SUCCESS);
            } else {
                model.getCurrentQuestionIndex().set(qIdx + 1);
            }
        } else {
            model.getCurrentScreenState().set(CreateWalletVerifyModel.ScreenState.WRONG);
        }
    }

    public CreateWalletVerifyModel getModel() {
        return model;
    }

    public void setSeedWords(List<String> seedWords) {
        for (int i = 0; i < 12; i++) {
            model.getSeedWords()[i].set(seedWords.get(i));
        }
        model.setupQuestions(seedWords);
    }

    private void loadSeedWordsAsync(WalletService walletService) {
        model.getCurrentScreenState().set(CreateWalletVerifyModel.ScreenState.LOADING);
        walletService.getSeedWords()
                .thenAccept(seedWords ->
                        UIThread.run(() -> {
                            setSeedWords(seedWords);
                            model.getCurrentScreenState().set(CreateWalletVerifyModel.ScreenState.QUIZ);
                            log.error("loadSeedWordsAsync :: Verify :: Loaded seed words");
                        }))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

                    log.error("loadSeedWordsAsync :: Failed to load seed words", cause);
                    UIThread.run(() -> model.getCurrentScreenState().set(CreateWalletVerifyModel.ScreenState.ERROR));

                    new Popup().invalid("Error loading seed. Try again")
                            .owner((Region) view.getRoot().getParent().getParent())
                            .show();

                    return null;
                });
    }

} 