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

package bisq.desktop.main.content.wallet.setup_wallet_wizard;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.View;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.protect.SetupWalletWizardProtectController;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.backup.SetupWalletWizardBackupController;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.setup_or_restore.SetupWalletWizardSetupOrRestoreController;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.verify.SetupWalletWizardVerifyController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.wallet.WalletService;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SetupWalletWizardController extends NavigationController {
    private final OverlayController overlayController;
    @Getter
    private final SetupWalletWizardModel model;
    private final SetupWalletWizardView view;

    private final SetupWalletWizardSetupOrRestoreController setupWalletWizardSetupOrRestoreController;
    private final SetupWalletWizardProtectController setupWalletWizardProtectController;
    private final SetupWalletWizardBackupController setupWalletWizardBackupController;
    private final SetupWalletWizardVerifyController setupWalletWizardVerifyController;
    private final WalletService walletService;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;

    public SetupWalletWizardController(ServiceProvider serviceProvider) {
        super(NavigationTarget.SETUP_WALLET);

        overlayController = OverlayController.getInstance();

        model = new SetupWalletWizardModel();
        view = new SetupWalletWizardView(model, this);

        setupWalletWizardSetupOrRestoreController = new SetupWalletWizardSetupOrRestoreController(serviceProvider);
        setupWalletWizardProtectController = new SetupWalletWizardProtectController(serviceProvider,
                this::onNext,
                this::setMainButtonsVisibleState);
        setupWalletWizardBackupController = new SetupWalletWizardBackupController(serviceProvider,
                this::setMainButtonsVisibleState,
                this::onBack);
        setupWalletWizardVerifyController = new SetupWalletWizardVerifyController(
                serviceProvider,
                this::setMainButtonsVisibleState,
                this::closeAndNavigateTo,
                this::onBack);

        this.walletService = serviceProvider.getWalletService().orElseThrow();
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public View<? extends Parent, ? extends Model, ? extends Controller> getView() {
        return view;
    }

    @Override
    public void onActivate() {
        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        model.getSelectedChildTarget().set(model.getChildTargets().get(0));
        model.getBackButtonText().set(Res.get("action.back"));
        model.getNextButtonVisible().set(true);
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        String nextString = "", backString = "";
        if (navigationTarget == NavigationTarget.SETUP_OR_RESTORE_WALLET) {
            backString = Res.get("wallet.setupOrRestoreWallet.backButton");
            nextString = Res.get("wallet.setupOrRestoreWallet.nextButton");
        } else if (navigationTarget == NavigationTarget.SETUP_WALLET_PROTECT) {
            backString = Res.get("action.back");
            nextString = Res.get("action.next");
        } else if (navigationTarget == NavigationTarget.SETUP_WALLET_BACKUP) {
            backString = Res.get("action.back");
            nextString = Res.get("wallet.backupSeeds.button.verify");
        } else if (navigationTarget == NavigationTarget.SETUP_WALLET_VERIFY) {
            backString = Res.get("action.back");
            nextString = Res.get("wallet.verifySeeds.button.question.nextWord");
        }

        model.getNextButtonText().set(nextString);
        model.getBackButtonText().set(backString);

        boolean shouldShowHeader = navigationTarget != NavigationTarget.SETUP_OR_RESTORE_WALLET;
        model.getShouldShowHeader().set(shouldShowHeader);

        boolean shouldShowSkipThisStep = navigationTarget == NavigationTarget.SETUP_WALLET_PROTECT;
        model.getSkipThisStepHyperLinkVisible().set(shouldShowSkipThisStep);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case SETUP_OR_RESTORE_WALLET -> Optional.of(setupWalletWizardSetupOrRestoreController);
            case SETUP_WALLET_PROTECT -> Optional.of(setupWalletWizardProtectController);
            case SETUP_WALLET_BACKUP -> Optional.of(setupWalletWizardBackupController);
            case SETUP_WALLET_VERIFY -> Optional.of(setupWalletWizardVerifyController);
            default -> Optional.empty();
        };
    }

    void onNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            NavigationTarget currentTarget = model.getNavigationTarget();
            log.info("Navigating from {} to index {}", currentTarget, nextIndex);
            if (currentTarget == NavigationTarget.SETUP_WALLET_PROTECT) {
                if (!setupWalletWizardProtectController.isValid()) {
                    log.warn("Protect step invalid input");
                    setupWalletWizardProtectController.handleInvalidInput();
                    return;
                }

                // Only encrypt if the user actually set a password (i.e., did not skip)
                if (!setupWalletWizardProtectController.getModel().isSkipProtectStep()) {
                    String password = setupWalletWizardProtectController.getPassword();
                    walletService.encryptWallet(password);
                }
            }
            model.setAnimateRightOut(false);
            model.getCurrentIndex().set(nextIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
            model.getSelectedChildTarget().set(nextTarget);
            model.getNextButtonDisabled().set(false);
            Navigation.navigateTo(nextTarget);
        }
    }

    void onBack() {
        int prevIndex = model.getCurrentIndex().get() - 1;
        if (prevIndex >= 0) {
            model.setAnimateRightOut(true);
            model.getCurrentIndex().set(prevIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(prevIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
        }
    }

    void onSkipThisStep() {
        if (model.getNavigationTarget() == NavigationTarget.SETUP_WALLET_PROTECT) {
            setupWalletWizardProtectController.handleSkipProtectStep();
        }
    }

    void onClose() {
        Navigation.navigateTo(NavigationTarget.MAIN);
        OverlayController.hide();
    }

    void onKeyPressed(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onClose);
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, this::onNext);
    }

    private void closeAndNavigateTo(NavigationTarget navigationTarget) {
        reset();
       // walletService.purgeSeedWords();
        walletService.initialize();
        OverlayController.hide(() -> Navigation.navigateTo(navigationTarget));
    }

    private void setMainButtonsVisibleState(boolean value) {
        model.getNextButtonVisible().set(value);
        model.getBackButtonVisible().set(value);
        boolean isProtectStep = model.getNavigationTarget() == NavigationTarget.SETUP_WALLET_PROTECT;
        model.getSkipThisStepHyperLinkVisible().set(isProtectStep && value);
    }

    private void reset() {
        resetSelectedChildTarget();
    }
}
