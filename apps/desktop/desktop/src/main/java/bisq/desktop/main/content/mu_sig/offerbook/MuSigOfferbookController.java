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
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.mu_sig.create_offer.MuSigCreateOfferController;
import bisq.desktop.main.content.mu_sig.take_offer.MuSigTakeOfferController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.settings.SettingsService;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Locale;
import java.util.Optional;

@Slf4j
public abstract class MuSigOfferbookController<M extends MuSigOfferbookModel, V extends MuSigOfferbookView<?, ?>> implements Controller {
    @Getter
    protected final V view;
    protected final M model;
    protected final MuSigService muSigService;
    protected final MarketPriceService marketPriceService;
    protected final UserProfileService userProfileService;
    protected final SettingsService settingsService;
    protected Pin offersPin;
    private Subscription selectedMarketPin;

    public MuSigOfferbookController(ServiceProvider serviceProvider, Direction direction) {
        muSigService = serviceProvider.getMuSigService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        settingsService = serviceProvider.getSettingsService();

        model = createAndGetModel(direction);
        view = createAndGetView();
    }

    protected abstract V createAndGetView();

    protected abstract M createAndGetModel(Direction direction);

    @Override
    public void onActivate() {
        model.getMarket().set(settingsService.getSelectedMarket().get());
        model.getMarkets().setAll(MarketRepository.getAllFiatMarkets());

        selectedMarketPin = EasyBind.subscribe(model.getMarket(), market -> {
            if (market != null) {
                model.getSelectedMarket().set(market);
                model.getPriceTableHeader().set(Res.get("muSig.offerbook.table.price", market.getMarketCodes()).toUpperCase(Locale.ROOT));
                String baseCurrencyCode = market.getBaseCurrencyCode();
                String quoteCurrencyCode = market.getQuoteCurrencyCode();

                if (model.getDirection().isBuy()) {
                    model.getAmountToReceive().set(Res.get("muSig.offerbook.table.amountToReceive", baseCurrencyCode).toUpperCase(Locale.ROOT));
                    model.getAmountToSend().set(Res.get("muSig.offerbook.table.amountToPay", quoteCurrencyCode).toUpperCase(Locale.ROOT));
                } else {
                    model.getAmountToReceive().set(Res.get("muSig.offerbook.table.amountToReceive", quoteCurrencyCode).toUpperCase(Locale.ROOT));
                    model.getAmountToSend().set(Res.get("muSig.offerbook.table.amountToSend", baseCurrencyCode).toUpperCase(Locale.ROOT));
                }

                updatePredicate();
            }
        });

        offersPin = muSigService.getObservableOffers().addObserver(new CollectionObserver<>() {
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

    private void updatePredicate() {
        Market market = model.getSelectedMarket().get();
        model.getFilteredList().setPredicate(item ->
                item.getOffer().getDirection() == model.getDirection().mirror() &&
                        item.getOffer().getMarket().equals(market)
        );
    }

    @Override
    public void onDeactivate() {
        selectedMarketPin.unsubscribe();
        offersPin.unbind();
        model.getListItems().forEach(MuSigOfferListItem::dispose);
        model.getListItems().clear();
        model.getOfferIds().clear();
    }

    void onCreateOffer() {
        Navigation.navigateTo(NavigationTarget.MU_SIG_CREATE_OFFER, new MuSigCreateOfferController.InitData(model.getDirection(), model.getMarket().get()));
    }

    void onTakeOffer(MuSigOffer offer) {
        Navigation.navigateTo(NavigationTarget.MU_SIG_TAKE_OFFER, new MuSigTakeOfferController.InitData(offer));
    }

    public void onSelectMarket(Market market) {
        model.getMarket().set(market);
    }
}
