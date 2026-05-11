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

package bisq.desktop.main.content.user.accounts.stable_coin_accounts.create;

import bisq.account.AccountService;
import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.stable_coin.StableCoinAccount;
import bisq.account.accounts.stable_coin.StableCoinAccountPayload;
import bisq.account.payment_method.stable_coin.StableCoinPaymentRail;
import bisq.account.timestamp.KeyType;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.security.keys.KeyBundleService;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;

@Slf4j
public class CreateStableCoinAccountController extends NavigationController {
    @Getter
    private final CreateStableCoinAccountModel model;
    @Getter
    private final CreateStableCoinAccountView view;
    private final OverlayController overlayController;
    private final AccountService accountService;
    private final KeyBundleService keyBundleService;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;

    private final StableCoinAccountDataController dataController;
    private final StableCoinAccountSummaryController summaryController;

    public CreateStableCoinAccountController(ServiceProvider serviceProvider) {
        super(NavigationTarget.CREATE_STABLE_COIN_ACCOUNT);

        accountService = serviceProvider.getAccountService();
        keyBundleService = serviceProvider.getSecurityService().getKeyBundleService();

        model = new CreateStableCoinAccountModel();
        dataController = new StableCoinAccountDataController(model);
        summaryController = new StableCoinAccountSummaryController(model);
        view = new CreateStableCoinAccountView(model, this);

        overlayController = OverlayController.getInstance();
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public void onActivate() {
        model.getNextButtonVisible().set(true);
        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        model.getChildTargets().clear();
        model.getChildTargets().add(NavigationTarget.CREATE_STABLE_COIN_ACCOUNT_DATA);
        model.getChildTargets().add(NavigationTarget.CREATE_STABLE_COIN_ACCOUNT_SUMMARY);

        model.getCurrentIndex().set(0);
        model.getSelectedChildTarget().set(model.getChildTargets().get(0));
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCreateAccountButtonVisible().set(navigationTarget == NavigationTarget.CREATE_STABLE_COIN_ACCOUNT_SUMMARY);
        model.getNextButtonVisible().set(navigationTarget != NavigationTarget.CREATE_STABLE_COIN_ACCOUNT_SUMMARY);
        model.getBackButtonVisible().set(model.getCurrentIndex().get() > 0);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case CREATE_STABLE_COIN_ACCOUNT_DATA -> Optional.of(dataController);
            case CREATE_STABLE_COIN_ACCOUNT_SUMMARY -> Optional.of(summaryController);
            default -> Optional.empty();
        };
    }

    void onKeyPressed(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onClose);
        KeyHandlerUtil.handleEnterKeyEventWithTextInputFocusCheck(keyEvent, getView().getRoot(), this::navigateNext);
    }

    void onNext() {
        navigateNext();
    }

    private void navigateNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size() && validate()) {
            model.setAnimateRightOut(false);
            navigateToIndex(nextIndex);
        }
    }

    void onBack() {
        int prevIndex = model.getCurrentIndex().get() - 1;
        if (prevIndex >= 0) {
            model.setAnimateRightOut(true);
            navigateToIndex(prevIndex);
        }
    }

    void onClose() {
        OverlayController.hide();
    }

    void onCreateAccount() {
        StableCoinPaymentRail rail = model.getSelectedRail().get();
        String address = model.getAddress().get().trim();
        String accountName = model.getAccountName().get().trim();

        if (rail == null || StringUtils.isEmpty(address)) {
            return;
        }

        String currencyCode = rail.getStableCoin().getCode();
        String network = rail.getStableCoin().getNetwork().name();
        String id = StringUtils.createUid();

        if (StringUtils.isEmpty(accountName)) {
            accountName = currencyCode + " (" + rail.getStableCoin().getNetwork().getDisplayName() + ") " + StringUtils.truncate(address, 8);
        }

        try {
            KeyPair keyPair = keyBundleService.generateKeyPair();
            StableCoinAccountPayload payload = new StableCoinAccountPayload(id, currencyCode, address, network);
            StableCoinAccount account = new StableCoinAccount(
                    id,
                    System.currentTimeMillis(),
                    accountName,
                    payload,
                    keyPair,
                    KeyType.EC,
                    AccountOrigin.BISQ2_NEW);
            accountService.addPaymentAccount(account);
            OverlayController.hide();
        } catch (Exception e) {
            log.error("Failed to create stablecoin account", e);
        }
    }

    private void navigateToIndex(int index) {
        model.getCurrentIndex().set(index);
        NavigationTarget target = model.getChildTargets().get(index);
        model.getSelectedChildTarget().set(target);
        Navigation.navigateTo(target);
    }

    private boolean validate() {
        return switch (model.getSelectedChildTarget().get()) {
            case CREATE_STABLE_COIN_ACCOUNT_DATA -> {
                StableCoinPaymentRail rail = model.getSelectedRail().get();
                String address = model.getAddress().get();
                yield rail != null && StringUtils.isNotEmpty(address) && StableCoinAccountPayload.isValidEvmAddress(address.trim());
            }
            default -> true;
        };
    }
}
