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
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.identity.IdentityService;
import bisq.mu_sig.MuSigService;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.settings.SettingsService;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

public class MuSigOfferbookController implements Controller {
    @Getter
    private final MuSigOfferbookView view;
    private final MuSigOfferbookModel model;
    protected final MuSigService muSigService;
    protected final MarketPriceService marketPriceService;
    protected final UserProfileService userProfileService;
    protected final SettingsService settingsService;
    private final IdentityService identityService;
    private final BannedUserService bannedUserService;
    protected Pin offersPin;

    public MuSigOfferbookController(ServiceProvider serviceProvider) {
        muSigService = serviceProvider.getMuSigService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        identityService = serviceProvider.getIdentityService();
        settingsService = serviceProvider.getSettingsService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();

        model = new MuSigOfferbookModel();
        view = new MuSigOfferbookView(model, this);
    }

    @Override
    public void onActivate() {
        offersPin = muSigService.getObservableOffers().addObserver(new CollectionObserver<>() {
            @Override
            public void add(MuSigOffer muSigOffer) {
                UIThread.run(() -> {
                    String offerId = muSigOffer.getId();
                    if (/*isExpectedMarket(muSigOffer.getMarket()) && */!model.getOfferIds().contains(offerId)) {
                        model.getListItems().add(new MuSigOfferListItem(muSigOffer, marketPriceService, userProfileService, identityService));
                        model.getOfferIds().add(offerId);
                        //updatePredicate();
                    }
                });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof MuSigOffer muSigOffer) {
                    UIThread.run(() -> {
                        String offerId = muSigOffer.getId();
                        Optional<MuSigOfferListItem> toRemove = model.getListItems().stream()
                                .filter(item -> item.getOffer().getId().equals(offerId))
                                .findAny();
                        toRemove.ifPresent(offer -> {
                            model.getListItems().remove(offer);
                            model.getOfferIds().remove(offerId);
                        });
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> {
                    model.getListItems().clear();
                    model.getOfferIds().clear();
                });
            }
        });
    }

    @Override
    public void onDeactivate() {
        offersPin.unbind();
        model.getListItems().forEach(MuSigOfferListItem::dispose);
        model.getListItems().clear();
        model.getOfferIds().clear();
    }
}
