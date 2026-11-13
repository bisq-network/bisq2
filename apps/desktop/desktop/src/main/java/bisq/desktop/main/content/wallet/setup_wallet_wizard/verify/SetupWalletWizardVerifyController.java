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
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.SeedWords;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.wallet.WalletService;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class SetupWalletWizardVerifyController implements Controller {
    @Getter
    private final SetupWalletWizardVerifyModel model;
    @Getter
    private final SetupWalletWizardVerifyView view;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final Runnable onBackHandler;
    private final Supplier<String> walletPassword;
    private final WalletService walletService;
    private boolean activated;

    public SetupWalletWizardVerifyController(ServiceProvider serviceProvider,
                                             Consumer<Boolean> navigationButtonsVisibleHandler,
                                             Consumer<NavigationTarget> closeAndNavigateToHandler,
                                             Runnable onBackHandler,
                                             Supplier<String> walletPassword) {
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        this.onBackHandler = onBackHandler;
        this.walletPassword = walletPassword;

        model = new SetupWalletWizardVerifyModel();
        view = new SetupWalletWizardVerifyView(model, this);

        walletService = serviceProvider.getWalletService().orElseThrow();
    }

    @Override
    public void onActivate() {
        activated = true;
        navigationButtonsVisibleHandler.accept(false);
        model.reset();
        loadSeedWordsAsync(walletService);
    }

    @Override
    public void onDeactivate() {
        activated = false;
        navigationButtonsVisibleHandler.accept(true);
    }

    void onAnswerSelected(int idx) {
        model.getSelectedAnswerIndex().set(idx);
    }

    void onNextWordSelected() {
        int qIdx = model.getCurrentQuestionIndex().get();
        int selectedIdx = model.getSelectedAnswerIndex().get();
        if (selectedIdx == -1) {
            return;
        }
        int correctIdx = model.getCorrectAnswerIndices().get(qIdx);
        if (selectedIdx == correctIdx) {
            if (qIdx == SetupWalletWizardVerifyModel.QUESTIONS_COUNT - 1) {
                model.getCurrentScreenState().set(SetupWalletWizardVerifyModel.ScreenState.SUCCESS);
            } else {
                model.getShouldTransitionToNextQuestion().set(true);
            }
        } else {
            model.getShouldShowWrongWordOverlay().set(true);
        }
    }

    void onKeyPressedWhileShowingSuccessOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, this::onCreateWallet);
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, () -> {});
    }

    void onKeyPressedWhileShowingWrongWordOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, this::onWrongWord);
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, () -> {});
    }

    void onWrongWord() {
        onBackHandler.run();
        model.getShouldShowWrongWordOverlay().set(false);
    }

    void onCreateWallet() {
        closeAndNavigateToHandler.accept(NavigationTarget.WALLET);
    }

    void onGoToNextQuestion() {
        int qIdx = model.getCurrentQuestionIndex().get();
        model.getCurrentQuestionIndex().set(qIdx + 1);
        model.getShouldTransitionToNextQuestion().set(false);
    }

    private void loadSeedWordsAsync(WalletService walletService) {
        walletService.getSeedWords(walletPassword.get()).thenAccept(seedWords -> {
            SeedWords.requireValidCount(seedWords);
            UIThread.run(() -> model.setupQuestions(seedWords));
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

            log.error("loadSeedWordsAsync :: Failed to load seed words", cause);
            UIThread.run(() -> {
                if (!activated) {
                    return;
                }
                new Popup().invalid(Res.get("wallet.backupSeeds.error.failedToLoad"))
                        .owner(getPopupOwner())
                        .show();
            });

            return null;
        });
    }

    private Region getPopupOwner() {
        return (Region) view.getRoot().getParent().getParent();
    }
}
