package bisq.desktop.main.content.wallet.create_wallet;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.wallet.create_wallet.protect.CreateWalletProtectController;
import bisq.desktop.main.content.wallet.create_wallet.backup.CreateWalletBackupController;
import bisq.desktop.main.content.wallet.create_wallet.protect.CreateWalletProtectModel;
import bisq.desktop.main.content.wallet.create_wallet.verify.CreateWalletVerifyController;
import bisq.desktop.main.content.wallet.create_wallet.verify.CreateWalletVerifyModel;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.wallets.core.WalletService;
import bisq.wallets.core.MockWalletService;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.Optional;

@Slf4j
public class CreateWalletController extends NavigationController {
    private final OverlayController overlayController;
    @Getter
    private final CreateWalletModel model;
    @Getter
    private final CreateWalletView view;

    private final CreateWalletProtectController createWalletProtectController;
    private final CreateWalletBackupController createWalletBackupController;
    private final CreateWalletVerifyController createWalletVerifyController;
    private final WalletService walletService;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;

    private ChangeListener<CreateWalletVerifyModel.ScreenState> verifyScreenStateListener;

    public CreateWalletController(ServiceProvider serviceProvider) {
        super(NavigationTarget.CREATE_WALLET);

        overlayController = OverlayController.getInstance();

        model = new CreateWalletModel();
        view = new CreateWalletView(model, this);

        createWalletProtectController = new CreateWalletProtectController(serviceProvider);
        createWalletBackupController = new CreateWalletBackupController(serviceProvider, this::setMainButtonsVisibleState);
        createWalletVerifyController = new CreateWalletVerifyController(serviceProvider, this::setMainButtonsVisibleState);

        verifyScreenStateListener = (obs, oldState, newState) -> {
            if (newState == CreateWalletVerifyModel.ScreenState.QUIZ) {
                setMainButtonsVisibleState(false);
            } else if (newState == CreateWalletVerifyModel.ScreenState.SUCCESS) {
                model.getNextButtonVisible().set(true);
                model.getNextButtonText().set("Go to wallet");
            } else if (newState == CreateWalletVerifyModel.ScreenState.WRONG) {
                model.getNextButtonVisible().set(true);
                model.getNextButtonText().set("View seed words");
            }
        };

        this.walletService = serviceProvider.getWalletService().orElseThrow();
    }

    @Override
    public boolean useCaching() { return false; }

    @Override
    public void onActivate() {
        model.getChildTargets().clear();
        model.getChildTargets().add(NavigationTarget.CREATE_WALLET_PROTECT);
        model.getChildTargets().add(NavigationTarget.CREATE_WALLET_BACKUP);
        model.getChildTargets().add(NavigationTarget.CREATE_WALLET_VERIFY);

        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        model.getSelectedChildTarget().set(model.getChildTargets().get(0));
        model.getBackButtonText().set(Res.get("action.back"));
        model.getNextButtonVisible().set(true);

        createWalletVerifyController.getModel().getCurrentScreenState().addListener(verifyScreenStateListener);
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);
        createWalletVerifyController.getModel().getCurrentScreenState().removeListener(verifyScreenStateListener);
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        String nextString = "";
        if (navigationTarget == NavigationTarget.CREATE_WALLET_PROTECT) {
            nextString = Res.get("wallet.protectWallet.button.nextStep");
        } else if (navigationTarget == NavigationTarget.CREATE_WALLET_BACKUP ) {
            nextString = Res.get("wallet.backupSeeds.button.verify");
        } else if (navigationTarget == NavigationTarget.CREATE_WALLET_VERIFY ) {
            nextString = "Next word";
        }
        model.getNextButtonText().set(nextString);

        String backString = "";
        if (navigationTarget == NavigationTarget.CREATE_WALLET_PROTECT) {
            backString = Res.get("wallet.protectWallet.button.skipStep");
        } else if (navigationTarget == NavigationTarget.CREATE_WALLET_BACKUP) {
            backString = Res.get("action.back");
        } else if (navigationTarget == NavigationTarget.CREATE_WALLET_VERIFY) {
            backString = Res.get("action.back");
        }
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

    void onNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            handleStepTransition(nextIndex);
        } else if (nextIndex == model.getChildTargets().size()) {
            handleFinalStep();
        }
    }

    private void handleStepTransition(int nextIndex) {
        NavigationTarget currentTarget = model.getNavigationTarget();
        log.info("Navigating from {} to index {}", currentTarget, nextIndex);
        if (currentTarget == NavigationTarget.CREATE_WALLET_PROTECT) {
            if (!createWalletProtectController.isValid()) {
                log.warn("Protect step invalid input");
                createWalletProtectController.handleInvalidInput();
                return;
            }
            CreateWalletProtectModel protectModel = createWalletProtectController.getModel();
            walletService.setEncryptionPassword(protectModel.getPassword().get());
        }
        model.setAnimateRightOut(false);
        model.getCurrentIndex().set(nextIndex);
        NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
        model.getSelectedChildTarget().set(nextTarget);
        Navigation.navigateTo(nextTarget);
    }

    private void handleFinalStep() {
        NavigationTarget currentTarget = model.getNavigationTarget();
        log.info("Handling final step for {}", currentTarget);
        if (currentTarget == NavigationTarget.CREATE_WALLET_VERIFY) {
            CreateWalletVerifyModel.ScreenState state = createWalletVerifyController.getModel().getCurrentScreenState().get();
            if (state == CreateWalletVerifyModel.ScreenState.SUCCESS) {
                log.info("Wallet verified successfully, initializing wallet");
                walletService.initializeWallet(null, Optional.empty());
                OverlayController.hide();
            } else if (state == CreateWalletVerifyModel.ScreenState.WRONG) {
                log.warn("Verification failed, returning to previous step");
                onBack();
            }
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
        int nextIndex = model.getCurrentIndex().get() + 1;
        log.info("Skipping protect step, moving to index {}", nextIndex);
        model.setAnimateRightOut(false);
        model.getCurrentIndex().set(nextIndex);
        NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
        model.getSelectedChildTarget().set(nextTarget);
        Navigation.navigateTo(nextTarget);
    }

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
        OverlayController.hide(() -> Navigation.navigateTo(navigationTarget));
    }

    private void setMainButtonsVisibleState(boolean value) {
        model.getNextButtonVisible().set(value);
        model.getBackButtonVisible().set(value);

    }
}
