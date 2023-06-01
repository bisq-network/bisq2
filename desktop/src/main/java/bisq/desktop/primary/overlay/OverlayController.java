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

package bisq.desktop.primary.overlay;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.BisqEasyHelpController;
import bisq.desktop.primary.main.content.user.accounts.create.CreatePaymentAccountController;
import bisq.desktop.primary.main.content.user.reputation.accountAge.AccountAgeController;
import bisq.desktop.primary.main.content.user.reputation.bond.BondedReputationController;
import bisq.desktop.primary.main.content.user.reputation.burn.BurnBsqController;
import bisq.desktop.primary.main.content.user.reputation.signedAccount.SignedWitnessController;
import bisq.desktop.primary.main.content.user.userProfile.create.CreateUserProfileController;
import bisq.desktop.primary.overlay.bisqeasy.createoffer.CreateOfferController;
import bisq.desktop.primary.overlay.onboarding.OnboardingController;
import bisq.desktop.primary.overlay.tac.TacController;
import bisq.desktop.primary.overlay.unlock.UnlockController;
import javafx.application.Platform;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Popup for usage for views using the MVC pattern. It is a singleton created by the PrimaryStageController.
 * To add content to the popup we use the Navigation framework. For added views it is transparent if they are used
 * in normal parent views or in a popup.
 */
@Slf4j
public class OverlayController extends NavigationController {
    private static OverlayController INSTANCE;

    public static OverlayController getInstance() {
        return INSTANCE;
    }

    public static void hide() {
        hide(null);
    }

    public static void hide(@Nullable Runnable onHiddenHandler) {
        INSTANCE.onHiddenHandler = onHiddenHandler;
        INSTANCE.resetSelectedChildTarget();
    }

    public static void setTransitionsType(Transitions.Type transitionsType) {
        INSTANCE.getModel().setTransitionsType(transitionsType);
    }

    @Getter
    private final OverlayModel model;
    @Getter
    private final OverlayView view;
    @Getter
    private final Region applicationRoot;
    private final DefaultApplicationService applicationService;
    @Nullable
    private Runnable onHiddenHandler;

    public OverlayController(DefaultApplicationService applicationService, Region applicationRoot) {
        super(NavigationTarget.OVERLAY);

        this.applicationService = applicationService;
        this.applicationRoot = applicationRoot;

        model = new OverlayModel(applicationService);
        view = new OverlayView(model, this, applicationRoot);
        INSTANCE = this;
        onActivateInternal();

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                resetSelectedChildTarget();
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //  Controller implementation
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }


    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case UNLOCK: {
                return Optional.of(new UnlockController(applicationService));
            }
            case TAC: {
                return Optional.of(new TacController(applicationService));
            }
            case ONBOARDING: {
                return Optional.of(new OnboardingController(applicationService));
            }
            case CREATE_OFFER: {
                return Optional.of(new CreateOfferController(applicationService));
            }
            case BISQ_EASY_GUIDE: {
                return Optional.of(new BisqEasyHelpController(applicationService));
            }
            case CREATE_PROFILE: {
                return Optional.of(new CreateUserProfileController(applicationService));
            }
            case CREATE_BISQ_EASY_PAYMENT_ACCOUNT: {
                return Optional.of(new CreatePaymentAccountController(applicationService));
            }
            case BURN_BSQ: {
                return Optional.of(new BurnBsqController(applicationService));
            }
            case BSQ_BOND: {
                return Optional.of(new BondedReputationController(applicationService));
            }
            case ACCOUNT_AGE: {
                return Optional.of(new AccountAgeController(applicationService));
            }
            case SIGNED_WITNESS: {
                return Optional.of(new SignedWitnessController(applicationService));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onShown() {
    }

    void onHidden() {
        resetSelectedChildTarget();
        if (onHiddenHandler != null) {
            onHiddenHandler.run();
            onHiddenHandler = null;
        }
    }

    void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }
}
