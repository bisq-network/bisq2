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

package bisq.desktop.main.content.mu_sig.offer.create_offer.direction_and_market;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.desktop.main.content.mu_sig.offer.listing.MarketType;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOfferbookService;
import bisq.offer.mu_sig.draft.CreateOfferDraftWorkflow;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static bisq.desktop.main.content.mu_sig.offer.create_offer.direction_and_market.MuSigCreateOfferDirectionAndMarketModel.MARKET_ICON_CACHE;

@Slf4j
public class MuSigCreateOfferDirectionAndMarketController implements Controller {
    private final MuSigCreateOfferDirectionAndMarketModel model;
    @Getter
    private final MuSigCreateOfferDirectionAndMarketView view;
    private final CreateOfferDraftWorkflow createOfferDraftWorkflow;
    private final Runnable onNextHandler;
    private final MarketPriceService marketPriceService;
    private final MuSigOfferbookService muSigOfferbookService;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final Set<Pin> pins = new HashSet<>();

    public MuSigCreateOfferDirectionAndMarketController(ServiceProvider serviceProvider,
                                                        CreateOfferDraftWorkflow createOfferDraftWorkflow,
                                                        Runnable onNextHandler) {
        this.createOfferDraftWorkflow = createOfferDraftWorkflow;
        this.onNextHandler = onNextHandler;
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        muSigOfferbookService = serviceProvider.getOfferService().getMuSigOfferService().getMuSigOfferbookService();

        List<MarketTypeListItem> marketTypeListItems = List.of(
                new MarketTypeListItem(MarketType.FIAT),
                new MarketTypeListItem(MarketType.OTHER)
        );
        model = new MuSigCreateOfferDirectionAndMarketModel(marketTypeListItems);
        view = new MuSigCreateOfferDirectionAndMarketView(model, this);
    }

    @Override
    public void onActivate() {
        model.getPaymentCurrencySearchText().set("");

        pins.add(FxBindings.bind(model.getDirection()).to(createOfferDraftWorkflow.directionObservable()));

        pins.add(createOfferDraftWorkflow.marketObservable().addObserver(market
                -> UIThread.run(() -> {
            String baseCurrencyName = market.getBaseCurrencyName();
            String quoteCurrencyName = market.getQuoteCurrencyName();
            model.getHeadlineText().set(Res.get("muSig.offer.create.directionAndMarket.headline", baseCurrencyName, quoteCurrencyName));
            model.getBuyButtonText().set(Res.get("muSig.offer.create.directionAndMarket.buyButton", baseCurrencyName));
            model.getSellButtonText().set(Res.get("muSig.offer.create.directionAndMarket.sellButton", baseCurrencyName));

            StackPane marketIcons = MarketImageComposition.getMarketIcons(market, MARKET_ICON_CACHE);
            model.getTradePairImage().set(marketIcons);

            model.getSelectedMarketTypeListItem().set(new MarketTypeListItem(MarketType.from(market)));
        })));

        subscriptions.add(EasyBind.subscribe(model.getSelectedMarketTypeListItem(), selectedMarketTypeListItem -> {
            if (selectedMarketTypeListItem == null) {
                return;
            }

            Market selectedMarket = createOfferDraftWorkflow.getMarket();
            List<Market> markets;
            if (selectedMarketTypeListItem.getMarketType() == MarketType.FIAT) {
                markets = MarketRepository.getAllFiatMarkets();
            } else {
                markets = MarketRepository.getAllCryptoAssetMarkets();
            }
            List<MarketListItem> items = markets.stream()
                    .filter(marketPriceService::hasMarketPrice)
                    .map(market -> {
                        // We do not update num offers dynamically as the selection drop down is not expected to
                        // stay open long
                        long numOffersInMarket = muSigOfferbookService.getOffersForMarket(market).size();
                        MarketListItem item = new MarketListItem(market, numOffersInMarket);
                        if (market.equals(selectedMarket)) {
                            model.getSelectedMarketListItem().set(item);
                        }
                        return item;
                    })
                    .collect(Collectors.toList());
            model.getMarketListItems().setAll(items);
        }));

        subscriptions.add(EasyBind.subscribe(model.getPaymentCurrencySearchText(), searchText -> {
            if (StringUtils.isEmpty(searchText)) {
                model.getFilteredMarketListItems().setPredicate(item -> true);
            } else {
                String search = searchText.toLowerCase();
                model.getFilteredMarketListItems().setPredicate(item ->
                        item.getDisplayString().toLowerCase().contains(search)
                );
            }
        }));
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        pins.forEach(Pin::unbind);
        pins.clear();
    }

    void onSelectDirection(Direction direction) {
        createOfferDraftWorkflow.setDirection(direction);
        onNextHandler.run();
    }

    void onSelectMarketListItem(MarketListItem item) {
        if (item != null) {
            createOfferDraftWorkflow.setMarket(item.getMarket());
        }
    }

    void onSelectMarketTypeListItem(MarketTypeListItem listItem) {
        if (listItem != null) {
            model.getSelectedMarketTypeListItem().set(listItem);
        }
    }

    private Optional<MarketListItem> findMarketListItem(Market market) {
        return model.getMarketListItems().stream()
                .filter(m -> m.getMarket().equals(market))
                .findAny();
    }
}
