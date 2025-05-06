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
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.navigation.NavigationTarget;
import bisq.mu_sig.MuSigService;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigOfferbookController implements Controller {
    @Getter
    private final MuSigOfferbookView view;
    private final MuSigOfferbookModel model;
    private final MuSigService muSigService;
    private final MarketPriceService marketPriceService;
    private final UserProfileService userProfileService;
    private final ServiceProvider serviceProvider;

    public MuSigOfferbookController(ServiceProvider serviceProvider) {
        muSigService = serviceProvider.getMuSigService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        this.serviceProvider = serviceProvider;

        model = new MuSigOfferbookModel();
        view = new MuSigOfferbookView(model, this);
    }

    @Override
    public void onActivate() {
        muSigService.getObservableOffers().addObserver(new CollectionObserver<>() {
            @Override
            public void add(MuSigOffer muSigOffer) {
                UIThread.run(() -> {
                    String offerId = muSigOffer.getId();
                    if (!model.getOfferIds().contains(offerId)) {
                        model.getListItems().add(new MuSigOfferListItem(muSigOffer, marketPriceService, userProfileService));
                        model.getOfferIds().add(offerId);
                    }
                });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof MuSigOffer muSigOffer) {
                    UIThread.run(() -> {
                        String offerId = muSigOffer.getId();
                        Optional<MuSigOfferListItem> toRemove = model.getListItems().stream()
                                .filter(e -> e.getOfferId().equals(offerId))
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
        model.getListItems().forEach(MuSigOfferListItem::dispose);
        model.getListItems().clear();
        model.getOfferIds().clear();
    }

    void onCreateOffer() {
        Navigation.navigateTo(NavigationTarget.MU_SIG_CREATE_OFFER);
        /*
        Direction direction = Direction.BUY;
        Market market = MarketRepository.getUSDBitcoinMarket();
        AmountSpec amountSpec = new BaseSideFixedAmountSpec(500000 + new Random().nextInt(500000));
        PriceSpec priceSpec = new MarketPriceSpec();
        List<FiatPaymentMethod> fiatPaymentMethods = List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE));
        List<OfferOption> offerOptions = List.of();
        MuSigOffer offer = muSigService.createAndGetMuSigOffer(direction,
                market,
                amountSpec,
                priceSpec,
                fiatPaymentMethods,
                offerOptions);
        muSigService.publishOffer(offer).whenComplete((result, throwable) -> {
            if (throwable == null) {
                log.error("Offer publishing. {}", result);
                result.forEach(future -> {
                    future.whenComplete((res, t) -> {
                        if (t == null) {
                            log.error("Offer published. result={}", res);
                        } else {
                            log.error("Offer publishing failed with", throwable);
                        }
                    });
                });
            } else {
                log.error("Offer publishing failed", throwable);
            }
        });
        */

    }

    void onTakeOffer(MuSigOffer offer) {
        muSigService.takeOffer(offer);
    }
}
