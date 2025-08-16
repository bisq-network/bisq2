package bisq.desktop.main.content.wallet.create_wallet_wizard;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.View;
import bisq.desktop.main.content.wallet.create_wallet_wizard.protect.CreateWalletProtectController;
import bisq.desktop.main.content.wallet.create_wallet_wizard.backup.CreateWalletBackupController;
import bisq.desktop.main.content.wallet.create_wallet_wizard.verify.CreateWalletVerifyController;
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
public class CreateWalletController extends NavigationController {
    private final OverlayController overlayController;
    @Getter
    private final CreateWalletModel model;
    private final CreateWalletView view;

    private final CreateWalletProtectController createWalletProtectController;
    private final CreateWalletBackupController createWalletBackupController;
    private final CreateWalletVerifyController createWalletVerifyController;
    private final WalletService walletService;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;

    public CreateWalletController(ServiceProvider serviceProvider) {
        super(NavigationTarget.CREATE_WALLET);

        overlayController = OverlayController.getInstance();

        model = new CreateWalletModel();
        view = new CreateWalletView(model, this);

        createWalletProtectController = new CreateWalletProtectController(serviceProvider);
        createWalletBackupController = new CreateWalletBackupController(serviceProvider,
                this::setMainButtonsVisibleState,
                this::onBack
        );
        createWalletVerifyController = new CreateWalletVerifyController(
                serviceProvider,
                this::setMainButtonsVisibleState,
                this::closeAndNavigateTo,
                this::onBack
        );

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
        if (navigationTarget == NavigationTarget.CREATE_WALLET_PROTECT) {
            backString = Res.get("wallet.protectWallet.button.skipStep");
            nextString = Res.get("action.next");
        } else if (navigationTarget == NavigationTarget.CREATE_WALLET_BACKUP) {
            backString = Res.get("action.back");
            nextString = Res.get("wallet.backupSeeds.button.verify");
        } else if (navigationTarget == NavigationTarget.CREATE_WALLET_VERIFY) {
            backString = Res.get("action.back");
            nextString = Res.get("wallet.verifySeeds.button.question.nextWord");
        }

        model.getNextButtonText().set(nextString);
        model.getBackButtonText().set(backString);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case CREATE_WALLET_PROTECT -> Optional.of(createWalletProtectController);
            case CREATE_WALLET_BACKUP -> Optional.of(createWalletBackupController);
            case CREATE_WALLET_VERIFY -> Optional.of(createWalletVerifyController);
            default -> Optional.empty();
        };
    }

    // TODO: Generalise into OverlayWizardController
    void onNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            NavigationTarget currentTarget = model.getNavigationTarget();
            log.info("Navigating from {} to index {}", currentTarget, nextIndex);
            if (currentTarget == NavigationTarget.CREATE_WALLET_PROTECT) {
                if (!createWalletProtectController.isValid()) {
                    log.warn("Protect step invalid input");
                    createWalletProtectController.handleInvalidInput();
                    return;
                }
                String password = createWalletProtectController.getPassword();
                walletService.encryptWallet(password);
            }
            model.setAnimateRightOut(false);
            model.getCurrentIndex().set(nextIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
        }
    }

    void onBack() {
        int prevIndex = model.getCurrentIndex().get() - 1;
        if (prevIndex == -1) { // Handling 'Skip this step' in Protect your wallet
            handleSkipProtectStep();
        } else if (prevIndex >= 0) {
            handleStepBack(prevIndex);
        }
    }

    private void handleSkipProtectStep() {
        // Validate we're actually on the protection step
        if (model.getNavigationTarget() != NavigationTarget.CREATE_WALLET_PROTECT) {
            log.warn("Skip protection step called from invalid state: {}", model.getNavigationTarget());
            return;
        }
        int nextIndex = model.getCurrentIndex().get() + 1;
        log.info("Skipping protect step, moving to index {}", nextIndex);
        //walletService.setNoEncryption();
        model.setAnimateRightOut(false);
        model.getCurrentIndex().set(nextIndex);
        NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
        model.getSelectedChildTarget().set(nextTarget);
        Navigation.navigateTo(nextTarget);
    }

    // TODO: Generalise into OverlayWizardController
    private void handleStepBack(int prevIndex) {
        log.info("Navigating back to index {}", prevIndex);
        model.setAnimateRightOut(true);
        model.getCurrentIndex().set(prevIndex);
        NavigationTarget nextTarget = model.getChildTargets().get(prevIndex);
        model.getSelectedChildTarget().set(nextTarget);
        Navigation.navigateTo(nextTarget);
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
    }

    private void reset() {
        resetSelectedChildTarget();
    }
}
