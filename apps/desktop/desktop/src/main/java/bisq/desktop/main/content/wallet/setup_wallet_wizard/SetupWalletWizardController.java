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
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.View;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.wallet.WalletOpenErrors;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.backup.SetupWalletWizardBackupController;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.protect.SetupWalletWizardProtectController;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.setup_or_restore.SetupWalletWizardSetupOrRestoreController;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.verify.SetupWalletWizardVerifyController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.wallet.WalletService;
import com.google.common.annotations.VisibleForTesting;
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
    private boolean activated;
    private boolean walletOpenInProgress;
    // The password the daemon accepted. The later steps use it instead of the password field, which can change again.
    private String acceptedPassword = "";

    public SetupWalletWizardController(ServiceProvider serviceProvider) {
        super(NavigationTarget.SETUP_WALLET);

        overlayController = OverlayController.getInstance();

        model = new SetupWalletWizardModel();
        view = new SetupWalletWizardView(model, this);

        setupWalletWizardSetupOrRestoreController = new SetupWalletWizardSetupOrRestoreController(serviceProvider);
        setupWalletWizardProtectController = new SetupWalletWizardProtectController();
        setupWalletWizardBackupController = new SetupWalletWizardBackupController(serviceProvider,
                this::setMainButtonsVisibleState,
                this::onBack,
                () -> acceptedPassword);
        setupWalletWizardVerifyController = new SetupWalletWizardVerifyController(
                serviceProvider,
                this::setMainButtonsVisibleState,
                this::closeAndNavigateTo,
                this::onBack,
                () -> acceptedPassword);

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
        activated = true;
        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        model.getSelectedChildTarget().set(model.getChildTargets().get(0));
        model.getBackButtonText().set(Res.get("action.back"));
        model.getNextButtonVisible().set(true);
    }

    @Override
    public void onDeactivate() {
        activated = false;
        acceptedPassword = "";
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
            // The verify step hides the navigation buttons. Selecting an answer checks it and moves to the next word.
            backString = Res.get("action.back");
        }

        model.getNextButtonText().set(nextString);
        model.getBackButtonText().set(backString);

        boolean shouldShowHeader = navigationTarget != NavigationTarget.SETUP_OR_RESTORE_WALLET;
        model.getShouldShowHeader().set(shouldShowHeader);
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
        // A step hides Next while it is not done, for example the backup step while the seed words load. Enter
        // must not get past it either.
        if (!activated || walletOpenInProgress || !model.getNextButtonVisible().get()) {
            return;
        }
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            NavigationTarget currentTarget = model.getNavigationTarget();
            log.info("Navigating from {} to index {}", currentTarget, nextIndex);
            if (currentTarget == NavigationTarget.SETUP_WALLET_PROTECT) {
                if (!acceptedPassword.isEmpty()) {
                    // The wallet is open already, with the password which the locked fields still show
                    navigateTo(nextIndex);
                    return;
                }
                if (!setupWalletWizardProtectController.isValid()) {
                    log.warn("Protect step invalid input");
                    setupWalletWizardProtectController.handleInvalidInput();
                    return;
                }

                openWalletThenNavigateTo(nextIndex);
                return;
            }
            navigateTo(nextIndex);
        }
    }

    private void openWalletThenNavigateTo(int nextIndex) {
        String password = setupWalletWizardProtectController.getPassword();
        walletOpenInProgress = true;
        model.getNextButtonDisabled().set(true);
        model.getBackButtonDisabled().set(true);
        setupWalletWizardProtectController.setInputLocked(true);
        walletService.openOrCreateWallet(password).whenComplete((nothing, throwable) -> UIThread.run(() -> {
            walletOpenInProgress = false;
            model.getNextButtonDisabled().set(false);
            model.getBackButtonDisabled().set(false);
            // Once the daemon accepted the password, the wallet exists with it, and any other text would be refused.
            // The fields therefore stay locked for the rest of this setup.
            setupWalletWizardProtectController.setInputLocked(throwable == null);
            if (!activated) {
                return;
            }
            if (throwable == null) {
                acceptedPassword = password;
                navigateTo(nextIndex);
            } else {
                new Popup().warning(WalletOpenErrors.toMessage(throwable))
                        .owner(view.getRoot())
                        .show();
            }
        }));
    }

    private void navigateTo(int nextIndex) {
        model.setAnimateRightOut(false);
        model.getCurrentIndex().set(nextIndex);
        NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
        model.getSelectedChildTarget().set(nextTarget);
        model.getNextButtonDisabled().set(false);
        Navigation.navigateTo(nextTarget);
    }

    void onBack() {
        if (!activated || walletOpenInProgress) {
            return;
        }
        int prevIndex = model.getCurrentIndex().get() - 1;
        if (prevIndex >= 0) {
            // The step left behind may have hidden the buttons (backup while loading, verify); a step which needs
            // them hidden hides them again when it shows
            setMainButtonsVisibleState(true);
            model.setAnimateRightOut(true);
            model.getCurrentIndex().set(prevIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(prevIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
        }
    }

    // The overlay hides with an animation and deactivates the wizard only at its end. The first dismissal decides
    // where the user lands: a close, Escape, system close, Next, Back or open answer which arrives in between is
    // ignored, as it would open a second wizard or drop the target of the finish.
    void onClose() {
        if (!activated) {
            return;
        }
        activated = false;
        Navigation.navigateTo(NavigationTarget.MAIN);
        OverlayController.hide();
    }

    void onKeyPressed(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onClose);
        KeyHandlerUtil.handleEnterKeyEventWithTextInputFocusCheck(keyEvent, getView().getRoot(), this::onNext);
    }

    @VisibleForTesting
    void closeAndNavigateTo(NavigationTarget navigationTarget) {
        if (!activated) {
            return;
        }
        activated = false;
        reset();
        OverlayController.hide(() -> Navigation.navigateTo(navigationTarget));
    }

    private void setMainButtonsVisibleState(boolean value) {
        model.getNextButtonVisible().set(value);
        model.getBackButtonVisible().set(value);
    }

    private void reset() {
        resetSelectedChildTarget();
    }
}
