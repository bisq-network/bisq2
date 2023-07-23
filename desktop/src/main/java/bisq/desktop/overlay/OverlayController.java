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

package bisq.desktop.overlay;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.components.ReportToModeratorWindow;
import bisq.desktop.main.content.trade_apps.bisqEasy.chat.guide.BisqEasyGuideController;
import bisq.desktop.main.content.trade_apps.bisqEasy.chat.offer_details.BisqEasyOfferDetailsController;
import bisq.desktop.main.content.user.accounts.create.CreatePaymentAccountController;
import bisq.desktop.main.content.user.reputation.accountAge.AccountAgeController;
import bisq.desktop.main.content.user.reputation.bond.BondedReputationController;
import bisq.desktop.main.content.user.reputation.burn.BurnBsqController;
import bisq.desktop.main.content.user.reputation.signedAccount.SignedWitnessController;
import bisq.desktop.main.content.user.user_profile.create.CreateUserProfileController;
import bisq.desktop.overlay.bisq_easy.create_offer.CreateOfferController;
import bisq.desktop.overlay.bisq_easy.take_offer.TakeOfferController;
import bisq.desktop.overlay.onboarding.OnboardingController;
import bisq.desktop.overlay.tac.TacController;
import bisq.desktop.overlay.unlock.UnlockController;
import bisq.desktop.overlay.update.UpdaterController;
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
    private final ServiceProvider serviceProvider;
    @Nullable
    private Runnable onHiddenHandler;

    public OverlayController(ServiceProvider serviceProvider, Region applicationRoot) {
        super(NavigationTarget.OVERLAY);

        this.serviceProvider = serviceProvider;
        this.applicationRoot = applicationRoot;

        model = new OverlayModel(serviceProvider);
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
                return Optional.of(new UnlockController(serviceProvider));
            }
            case TAC: {
                return Optional.of(new TacController(serviceProvider));
            }
            case UPDATER: {
                return Optional.of(new UpdaterController(serviceProvider));
            }
            case ONBOARDING: {
                return Optional.of(new OnboardingController(serviceProvider));
            }
            case CREATE_OFFER: {
                return Optional.of(new CreateOfferController(serviceProvider));
            }
            case TAKE_OFFER: {
                return Optional.of(new TakeOfferController(serviceProvider));
            }
            case BISQ_EASY_GUIDE: {
                return Optional.of(new BisqEasyGuideController(serviceProvider));
            }
            case BISQ_EASY_OFFER_DETAILS: {
                return Optional.of(new BisqEasyOfferDetailsController(serviceProvider));
            }
            case CREATE_PROFILE: {
                return Optional.of(new CreateUserProfileController(serviceProvider));
            }
            case CREATE_BISQ_EASY_PAYMENT_ACCOUNT: {
                return Optional.of(new CreatePaymentAccountController(serviceProvider));
            }
            case BURN_BSQ: {
                return Optional.of(new BurnBsqController(serviceProvider));
            }
            case BSQ_BOND: {
                return Optional.of(new BondedReputationController(serviceProvider));
            }
            case ACCOUNT_AGE: {
                return Optional.of(new AccountAgeController(serviceProvider));
            }
            case SIGNED_WITNESS: {
                return Optional.of(new SignedWitnessController(serviceProvider));
            }
            case REPORT_TO_MODERATOR: {
                return Optional.of(new ReportToModeratorWindow(serviceProvider).getController());
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
        serviceProvider.getShotDownHandler().shutdown().thenAccept(result -> Platform.exit());
    }
}
