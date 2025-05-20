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

package bisq.desktop.main.content.mu_sig.offerbook;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.identity.IdentityService;
import bisq.mu_sig.MuSigService;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.settings.FavouriteMarketsService;
import bisq.settings.SettingsService;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfileService;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

public class MuSigOfferbookController implements Controller {
    @Getter
    private final MuSigOfferbookView view;
    private final MuSigOfferbookModel model;
    private final MuSigService muSigService;
    private final MarketPriceService marketPriceService;
    private final UserProfileService userProfileService;
    private final SettingsService settingsService;
    private final IdentityService identityService;
    private final BannedUserService bannedUserService;
    private final FavouriteMarketsService favouriteMarketsService;
    private Pin offersPin;

    public MuSigOfferbookController(ServiceProvider serviceProvider) {
        muSigService = serviceProvider.getMuSigService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        identityService = serviceProvider.getIdentityService();
        settingsService = serviceProvider.getSettingsService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        favouriteMarketsService = serviceProvider.getFavouriteMarketsService();

        model = new MuSigOfferbookModel();
        view = new MuSigOfferbookView(model, this);
    }

    @Override
    public void onActivate() {
        List<MarketChannelItem> marketChannelItems = MarketRepository.getAllFiatMarkets().stream()
                .map(market -> new MarketChannelItem(market,
                        favouriteMarketsService,
                        marketPriceService,
                        userProfileService,
                        muSigService))
                .toList();
        model.getMarketChannelItems().setAll(marketChannelItems);

        offersPin = muSigService.getObservableOffers().addObserver(new CollectionObserver<>() {
            @Override
            public void add(MuSigOffer muSigOffer) {
                UIThread.run(() -> {
                    String offerId = muSigOffer.getId();
                    if (/*isExpectedMarket(muSigOffer.getMarket()) && */!model.getMuSigOfferIds().contains(offerId)) {
                        model.getMuSigOfferListItems().add(new MuSigOfferListItem(muSigOffer, marketPriceService, userProfileService, identityService));
                        model.getMuSigOfferIds().add(offerId);
                        //updatePredicate();
                    }
                });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof MuSigOffer muSigOffer) {
                    UIThread.run(() -> {
                        String offerId = muSigOffer.getId();
                        Optional<MuSigOfferListItem> toRemove = model.getMuSigOfferListItems().stream()
                                .filter(item -> item.getOffer().getId().equals(offerId))
                                .findAny();
                        toRemove.ifPresent(offer -> {
                            model.getMuSigOfferListItems().remove(offer);
                            model.getMuSigOfferIds().remove(offerId);
                        });
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> {
                    model.getMuSigOfferListItems().clear();
                    model.getMuSigOfferIds().clear();
                });
            }
        });
    }

    @Override
    public void onDeactivate() {
        offersPin.unbind();
        model.getMuSigOfferListItems().forEach(MuSigOfferListItem::dispose);
        model.getMuSigOfferListItems().clear();
        model.getMuSigOfferIds().clear();
    }
}
