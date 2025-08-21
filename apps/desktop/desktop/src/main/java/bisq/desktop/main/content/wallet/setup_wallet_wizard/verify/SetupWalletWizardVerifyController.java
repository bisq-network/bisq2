/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.wallet.setup_wallet_wizard.verify;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.wallet.WalletService;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class SetupWalletWizardVerifyController implements Controller {
    private final SetupWalletWizardVerifyModel model;
    @Getter
    private final SetupWalletWizardVerifyView view;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final Runnable onBackHandler;
    private final WalletService walletService;

    public SetupWalletWizardVerifyController(ServiceProvider serviceProvider,
                                             Consumer<Boolean> navigationButtonsVisibleHandler,
                                             Consumer<NavigationTarget> closeAndNavigateToHandler,
                                             Runnable onBackHandler) {
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        this.onBackHandler = onBackHandler;

        model = new SetupWalletWizardVerifyModel();
        view = new SetupWalletWizardVerifyView(model, this);

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
            if (qIdx == SetupWalletWizardVerifyModel.QUESTIONS_COUNT - 1) { // Last Q
                // Use settings if needed
                //walletService.setIsWalletBackedup(true);
                model.getCurrentScreenState().set(SetupWalletWizardVerifyModel.ScreenState.SUCCESS);
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

    public SetupWalletWizardVerifyModel getModel() {
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
        walletService.setWalletInitialized();
        closeAndNavigateToHandler.accept(NavigationTarget.WALLET);
    }

    private Region getPopupOwner() {
        return (Region) view.getRoot().getParent().getParent();
    }

}
