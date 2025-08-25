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

package bisq.desktop.main.content.mu_sig.my_offers;

import bisq.account.AccountService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.mu_sig.MuSigOfferListItem;
import bisq.desktop.main.content.mu_sig.create_offer.MuSigCreateOfferController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.mu_sig.MuSigService;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.settings.SettingsService;
import bisq.user.banned.RateLimitExceededException;
import bisq.user.banned.UserProfileBannedException;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class MuSigMyOffersController implements Controller {
    @Getter
    private final MuSigMyOffersView view;
    private final MuSigMyOffersModel model;
    private final MuSigService muSigService;
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;
    private final UserProfileService userProfileService;
    private final IdentityService identityService;
    private final ReputationService reputationService;
    private final AccountService accountService;
    private final SettingsService settingsService;

    public MuSigMyOffersController(ServiceProvider serviceProvider) {
        muSigService = serviceProvider.getMuSigService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        identityService = serviceProvider.getIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        accountService = serviceProvider.getAccountService();
        settingsService = serviceProvider.getSettingsService();

        model = new MuSigMyOffersModel();
        view = new MuSigMyOffersView(model, this);
    }

    @Override
    public void onActivate() {
        UIThread.run(() -> {
            Set<String> myUserProfileIds = userIdentityService.getMyUserProfileIds();
            muSigService.getOffers().forEach(muSigOffer -> {
                boolean isMyOffer = myUserProfileIds.contains(muSigOffer.getMakersUserProfileId());
                if (isMyOffer) {
                    String offerId = muSigOffer.getId();
                    if (!model.getMuSigMyOffersIds().contains(offerId)) {
                        model.getMuSigMyOffersListItems().add(new MuSigOfferListItem(muSigOffer,
                                marketPriceService,
                                userProfileService,
                                identityService,
                                reputationService,
                                accountService));
                        model.getMuSigMyOffersIds().add(offerId);
                        updateFilteredMuSigMyOffersListItems();
                    }
                }
            });

            model.setNumOffers(Res.get("muSig.myOffers.numOffers", model.getMuSigMyOffersIds().size()));
            model.setShouldShowMyProfileColumn(myUserProfileIds.size() > 1);
        });
    }

    @Override
    public void onDeactivate() {
        model.getMuSigMyOffersListItems().forEach(MuSigOfferListItem::dispose);
        model.getMuSigMyOffersListItems().clear();
        model.getMuSigMyOffersIds().clear();
    }

    void onCreateOffer() {
        Market market = settingsService.getSelectedMuSigMarket().get();
        Navigation.navigateTo(NavigationTarget.MU_SIG_CREATE_OFFER,
                new MuSigCreateOfferController.InitData(market));
    }

    void onRemoveOffer(MuSigOffer muSigOffer) {
        new Popup().warning(Res.get("muSig.offerbook.removeOffer.confirmation"))
                .actionButtonText(Res.get("confirmation.yes"))
                .onAction(() -> doRemoveOffer(muSigOffer))
                .closeButtonText(Res.get("confirmation.no"))
                .show();
    }

    void applySearchPredicate(String searchText) {
        String string = searchText == null ? "" : searchText.toLowerCase();
        model.setSearchStringPredicate(item ->
                StringUtils.isEmpty(string)
                        || item.getMarket().getMarketDisplayName().toLowerCase().contains(string)
                        || item.getMakerUserProfile().getUserName().toLowerCase().contains(string)
                        || item.getOfferId().toLowerCase().contains(string)
                        || item.getOfferDate().toLowerCase().contains(string)
                        || item.getBaseAmountAsString().contains(string)
                        || item.getQuoteAmountAsString().contains(string)
                        || item.getPrice().contains(string)
                        || item.getPaymentMethodsAsString().toLowerCase().contains(string));
        applyPredicates();
    }

    private void applyPredicates() {
        model.getFilteredMuSigMyOffersListItems().setPredicate(null);
        model.getFilteredMuSigMyOffersListItems().setPredicate(model.getMuSigMyOffersListItemsPredicate());
    }

    private void doRemoveOffer(MuSigOffer muSigOffer) {
        try {
            muSigService.removeOffer(muSigOffer);
        } catch (UserProfileBannedException e) {
            UIThread.run(() -> {
                // We do not inform banned users about being banned
            });
        } catch (RateLimitExceededException e) {
            UIThread.run(() -> {
                new Popup().warning(Res.get("muSig.offerbook.rateLimitsExceeded.removeOffer.warning")).show();
            });
        }
    }

    private void updateFilteredMuSigMyOffersListItems() {
        model.getFilteredMuSigMyOffersListItems().setPredicate(null);
        model.getFilteredMuSigMyOffersListItems().setPredicate(model.getMuSigMyOffersListItemsPredicate());
    }
}
