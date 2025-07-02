package bisq.desktop.main.content.wallet.create_wallet.verify;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.wallet.create_wallet.verify.CreateWalletVerifyModel;
import bisq.desktop.main.content.wallet.create_wallet.verify.CreateWalletVerifyView;
import bisq.wallets.core.WalletService;
import javafx.scene.input.KeyEvent;
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

    public CreateWalletVerifyController(ServiceProvider serviceProvider,
                                         Consumer<Boolean> navigationButtonsVisibleHandler) {
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        model = new CreateWalletVerifyModel();
        WalletService walletService = serviceProvider.getWalletService().orElseThrow();
        setSeedWords(walletService.getSeedWords());
        view = new CreateWalletVerifyView(model, this);
    }

    public void setSeedWords(List<String> seedWords) {
        for (int i = 0; i < 12; i++) {
            model.getSeedWords()[i].set(seedWords.get(i));
        }
        model.setupQuestions(seedWords);
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

    @Override
    public void onActivate() {
        model.getCurrentQuestionIndex().set(0);
        model.getSelectedAnswerIndex().set(-1);
        model.getCurrentScreenState().set(CreateWalletVerifyModel.ScreenState.QUIZ);
        navigationButtonsVisibleHandler.accept(false);
    }

    @Override
    public void onDeactivate() {
        
    }

    void onKeyPressedWhileShowingOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
    }

    public CreateWalletVerifyModel getModel() {
        return model;
    }
} 