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
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.components.table.ShowTableInfo;
import bisq.desktop.main.content.authorized_role.mediator.details.MediationCaseDetailsController;
import bisq.desktop.main.content.bisq_easy.offerbook.offer_details.BisqEasyOfferDetailsController;
import bisq.desktop.main.content.bisq_easy.onboarding.video.BisqEasyVideoController;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_details.TradeDetailsController;
import bisq.desktop.main.content.bisq_easy.take_offer.TakeOfferController;
import bisq.desktop.main.content.bisq_easy.trade_guide.BisqEasyGuideController;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardController;
import bisq.desktop.main.content.bisq_easy.wallet_guide.WalletGuideController;
import bisq.desktop.main.content.components.AddToContactsListWindow;
import bisq.desktop.main.content.components.ReportToModeratorWindow;
import bisq.desktop.main.content.mu_sig.create_offer.MuSigCreateOfferController;
import bisq.desktop.main.content.mu_sig.open_trades.trade_details.MuSigTradeDetailsController;
import bisq.desktop.main.content.mu_sig.take_offer.MuSigTakeOfferController;
import bisq.desktop.main.content.reputation.build_reputation.accountAge.AccountAgeController;
import bisq.desktop.main.content.reputation.build_reputation.bond.BondedReputationController;
import bisq.desktop.main.content.reputation.build_reputation.burn.BurnBsqController;
import bisq.desktop.main.content.reputation.build_reputation.signedAccount.SignedWitnessController;
import bisq.desktop.main.content.user.crypto_accounts.create.CreateCryptoAssetAccountController;
import bisq.desktop.main.content.user.fiat_accounts.create.CreatePaymentAccountController;
import bisq.desktop.main.content.user.fiat_accounts.create.legacy.LegacyCreatePaymentAccountController;
import bisq.desktop.main.content.user.profile_card.ProfileCardController;
import bisq.desktop.main.content.user.user_profile.create.CreateUserProfileController;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.SetupWalletWizardController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.chat_rules.ChatRulesController;
import bisq.desktop.overlay.onboarding.OnboardingController;
import bisq.desktop.overlay.restore.RestoreFromBackupController;
import bisq.desktop.overlay.tac.TacController;
import bisq.desktop.overlay.unlock.UnlockController;
import bisq.desktop.overlay.update.UpdaterController;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.Setter;
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
    @Setter
    private boolean useEscapeKeyHandler;
    @Setter
    private Runnable enterKeyHandler;

    public OverlayController(ServiceProvider serviceProvider, Region applicationRoot) {
        super(NavigationTarget.OVERLAY);

        this.serviceProvider = serviceProvider;
        this.applicationRoot = applicationRoot;

        model = new OverlayModel(serviceProvider);
        view = new OverlayView(model, this, applicationRoot);
        INSTANCE = this;
        // We activate the OverlayController, and it stays active during the application lifetime
        onActivateInternal();

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                resetSelectedChildTarget();
            }
        });
    }


    /* --------------------------------------------------------------------- */
    //  Controller implementation
    /* --------------------------------------------------------------------- */

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }


    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case UNLOCK -> Optional.of(new UnlockController(serviceProvider));
            case TAC -> Optional.of(new TacController(serviceProvider));
            case UPDATER -> Optional.of(new UpdaterController(serviceProvider));
            case ONBOARDING -> Optional.of(new OnboardingController(serviceProvider));

            case RESTORE_FROM_BACKUP -> Optional.of(new RestoreFromBackupController(serviceProvider));

            case BISQ_EASY_VIDEO -> Optional.of(new BisqEasyVideoController(serviceProvider));
            case BISQ_EASY_GUIDE -> Optional.of(new BisqEasyGuideController(serviceProvider));
            case BISQ_EASY_TRADE_WIZARD -> Optional.of(new TradeWizardController(serviceProvider));
            case BISQ_EASY_TAKE_OFFER -> Optional.of(new TakeOfferController(serviceProvider));
            case BISQ_EASY_TRADE_DETAILS -> Optional.of(new TradeDetailsController(serviceProvider));
            case BISQ_EASY_OFFER_DETAILS -> Optional.of(new BisqEasyOfferDetailsController(serviceProvider));

            case SETUP_WALLET -> Optional.of(new SetupWalletWizardController(serviceProvider));

            case MU_SIG_CREATE_OFFER -> Optional.of(new MuSigCreateOfferController(serviceProvider));
            case MU_SIG_TAKE_OFFER -> Optional.of(new MuSigTakeOfferController(serviceProvider));
            case MU_SIG_TRADE_DETAILS -> Optional.of(new MuSigTradeDetailsController(serviceProvider));

            case WALLET_GUIDE -> Optional.of(new WalletGuideController(serviceProvider));
            case CHAT_RULES -> Optional.of(new ChatRulesController(serviceProvider));

            case CREATE_PROFILE -> Optional.of(new CreateUserProfileController(serviceProvider));
            case CREATE_PAYMENT_ACCOUNT -> Optional.of(new CreatePaymentAccountController(serviceProvider));
            case CREATE_PAYMENT_ACCOUNT_LEGACY ->
                    Optional.of(new LegacyCreatePaymentAccountController(serviceProvider));
            case CREATE_CRYPTO_CURRENCY_ACCOUNT -> Optional.of(new CreateCryptoAssetAccountController(serviceProvider));

            case BURN_BSQ -> Optional.of(new BurnBsqController(serviceProvider));
            case BSQ_BOND -> Optional.of(new BondedReputationController(serviceProvider));
            case ACCOUNT_AGE -> Optional.of(new AccountAgeController(serviceProvider));
            case SIGNED_WITNESS -> Optional.of(new SignedWitnessController(serviceProvider));
            case REPORT_TO_MODERATOR -> Optional.of(new ReportToModeratorWindow(serviceProvider).getController());

            case PROFILE_CARD -> Optional.of(new ProfileCardController(serviceProvider));
            case ADD_TO_CONTACTS_LIST -> Optional.of(new AddToContactsListWindow(serviceProvider).getController());
            case MEDIATION_CASE_DETAILS -> Optional.of(new MediationCaseDetailsController(serviceProvider));
            case SHOW_TABLE_INFO -> Optional.of(new ShowTableInfo(serviceProvider).getController());
            default -> Optional.empty();
        };
    }

    void onKeyPressed(KeyEvent keyEvent) {
        KeyHandlerUtil.handleShutDownKeyEvent(keyEvent, this::onQuit);

        if (useEscapeKeyHandler) {
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, () -> getView().hide());
        }
        if (enterKeyHandler != null) {
            KeyHandlerUtil.handleEnterKeyEvent(keyEvent, enterKeyHandler);
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
        serviceProvider.getShutDownHandler().shutdown();
    }
}
