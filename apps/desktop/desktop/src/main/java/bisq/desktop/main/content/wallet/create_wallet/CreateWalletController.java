package bisq.desktop.main.content.wallet.create_wallet;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.wallet.create_wallet.protect.CreateWalletProtectController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class CreateWalletController extends NavigationController {
    private final OverlayController overlayController;
    @Getter
    private final CreateWalletModel model;
    @Getter
    private final CreateWalletView view;

    private final CreateWalletProtectController createWalletProtectController;
    private final CreateWalletProtectController createWalletProtectController1;
    private final CreateWalletProtectController createWalletProtectController2;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;

    public CreateWalletController(ServiceProvider serviceProvider) {
        super(NavigationTarget.CREATE_WALLET);

        overlayController = OverlayController.getInstance();

        model = new CreateWalletModel();
        view = new CreateWalletView(model, this);

        createWalletProtectController = new CreateWalletProtectController(serviceProvider, this::setMainButtonsVisibleState);
        createWalletProtectController1 = new CreateWalletProtectController(serviceProvider, this::setMainButtonsVisibleState);
        createWalletProtectController2 = new CreateWalletProtectController(serviceProvider, this::setMainButtonsVisibleState);
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
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCloseButtonVisible().set(true);
        boolean isTakeOfferReview = navigationTarget == NavigationTarget.CREATE_WALLET_VERIFY;
        model.getNextButtonText().set(isTakeOfferReview ?
                Res.get("bisqEasy.takeOffer.review.takeOffer") :
                Res.get("action.next"));
        model.getShowProgressBox().set(!isTakeOfferReview);
        setMainButtonsVisibleState(true);
        model.getNextButtonVisible().set(!isTakeOfferReview);
    }


    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case CREATE_WALLET_PROTECT -> Optional.of(createWalletProtectController);
            case CREATE_WALLET_BACKUP -> Optional.of(createWalletProtectController1);
            case CREATE_WALLET_VERIFY -> Optional.of(createWalletProtectController2);
            default -> Optional.empty();
        };
    }

    void onNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            model.setAnimateRightOut(false);
            model.getCurrentIndex().set(nextIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
            model.getSelectedChildTarget().set(nextTarget);
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
        NavigationTarget navigationTarget = model.getNavigationTarget();
        model.getBackButtonVisible().set(value && model.getChildTargets().indexOf(navigationTarget) > 0);
        model.getNextButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.CREATE_WALLET_VERIFY);
        model.getCloseButtonVisible().set(value);
    }
}
