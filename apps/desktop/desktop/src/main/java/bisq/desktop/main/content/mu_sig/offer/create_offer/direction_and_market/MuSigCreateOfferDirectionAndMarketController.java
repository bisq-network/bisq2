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
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.desktop.main.content.mu_sig.offer.listing.MarketType;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import bisq.offer.Direction;
import bisq.offer.mu_sig.draft.CreateOfferDraftWorkflow;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.List;
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
    private final MuSigService muSigService;
    private final SettingsService settingsService;
    private Subscription paymentCurrencySearchTextPin, selectedMarketTypePin;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final Set<Pin> pins = new HashSet<>();

    public MuSigCreateOfferDirectionAndMarketController(ServiceProvider serviceProvider,
                                                        CreateOfferDraftWorkflow createOfferDraftWorkflow,
                                                        Runnable onNextHandler) {
        this.createOfferDraftWorkflow = createOfferDraftWorkflow;
        this.onNextHandler = onNextHandler;
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        muSigService = serviceProvider.getMuSigService();
        settingsService = serviceProvider.getSettingsService();

        model = new MuSigCreateOfferDirectionAndMarketModel(List.of(new MarketTypeListItem(MarketType.FIAT),
                new MarketTypeListItem(MarketType.OTHER)));
        view = new MuSigCreateOfferDirectionAndMarketView(model, this);
        selectDirection(Direction.BUY);
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        Market market = createOfferDraftWorkflow.getMarket();
        model.getTradePairImage().set(MarketImageComposition.getMarketIcons(market, MARKET_ICON_CACHE));

        model.getSelectedMarketTypeListItem().set(market.isCrypto()
                ? new MarketTypeListItem(MarketType.OTHER)
                : new MarketTypeListItem(MarketType.FIAT));
        selectDirection(Direction.BUY);

        model.getPaymentCurrencySearchText().set("");

        pins.add(createOfferDraftWorkflow.directionObservable().addObserver(direction -> {
            UIThread.run(() -> {
                model.getDirection().set(direction);
            });
        }));

        selectedMarketTypePin = EasyBind.subscribe(model.getSelectedMarketTypeListItem(), listItem -> {
            if (listItem == null) {
                return;
            }

            List<Market> markets;
            if (listItem.getMarketType() == MarketType.FIAT) {
                markets = MarketRepository.getAllFiatMarkets();
            } else {
                markets = MarketRepository.getAllCryptoAssetMarkets();
            }

            List<MarketListItem> items = markets.stream()
                    .filter(m -> marketPriceService.getMarketPriceByCurrencyMap().containsKey(m))
                    .map(m -> {
                        long numOffersInMarket = muSigService.getOffers().stream()
                                .filter(offer -> offer.getMarket().getRelevantCurrencyCode().equals(m.getRelevantCurrencyCode()))
                                .count();
                        MarketListItem item = new MarketListItem(m, numOffersInMarket);
                        if (m.equals(market)) {
                            model.getSelectedMarketListItem().set(item);
                        }
                        return item;
                    })
                    .collect(Collectors.toList());
            model.getMarketListItems().setAll(items);
        });

        paymentCurrencySearchTextPin = EasyBind.subscribe(model.getPaymentCurrencySearchText(), searchText -> {
            if (searchText == null || searchText.isEmpty()) {
                model.getFilteredMarketListItems().setPredicate(item -> true);
            } else {
                String search = searchText.toLowerCase();
                model.getFilteredMarketListItems().setPredicate(item ->
                        item != null &&
                                (item.getDisplayString().toLowerCase().contains(search) ||
                                        item.getMarket().getQuoteCurrencyDisplayName().toLowerCase().contains(search))
                );
            }
        });


        initializeMarketSelection();
    }

    @Override
    public void onDeactivate() {  subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        pins.forEach(Pin::unbind);
        pins.clear();
        paymentCurrencySearchTextPin.unsubscribe();
        selectedMarketTypePin.unsubscribe();
    }

    void onSelectDirection(Direction direction) {
        selectDirection(direction);
        onNextHandler.run();
    }

    void onMarketListItemClicked(MarketListItem item) {
        if (item == null || item.equals(model.getSelectedMarketListItem().get())) {
            return;
        }
        setSelectedMarketAndListItem(item.getMarket());
    }

    void onMarketTypeListItemSelected(MarketTypeListItem listItem) {
        if (listItem == null || listItem.equals(model.getSelectedMarketTypeListItem().get())) {
            return;
        }
        model.getSelectedMarketTypeListItem().set(listItem);

        updateWithLastSelectedOrDefaultMarket(listItem.getMarketType());
    }

    private void updateWithLastSelectedOrDefaultMarket(MarketType marketType) {
        if (marketType == MarketType.FIAT) {
            setSelectedMarketAndListItem(settingsService.getMuSigLastSelectedFiatMarket().get());
        } else {
            setSelectedMarketAndListItem(settingsService.getMuSigLastSelectedOtherMarket().get());
        }
    }

    private void setSelectedMarketAndListItem(Market market) {
        if (market != null) {
            updateSelectedMarket(market);
            settingsService.setSelectedMuSigMarket(market);
            if (market.isBtcFiatMarket()) {
                settingsService.setMuSigLastSelectedFiatMarket(market);
            } else {
                settingsService.setMuSigLastSelectedOtherMarket(market);
            }
        }
    }

    private void initializeMarketSelection() {
        Market market = settingsService.getSelectedMuSigMarket().get();
        if (market != null) {
            updateSelectedMarket(market);
            if (market.isBtcFiatMarket()) {
                model.getSelectedMarketTypeListItem().set(new MarketTypeListItem(MarketType.FIAT));
            } else {
                model.getSelectedMarketTypeListItem().set(new MarketTypeListItem(MarketType.OTHER));
            }
        } else {
            updateWithLastSelectedOrDefaultMarket(MarketType.FIAT);
        }
    }

    private void updateSelectedMarket(Market market) {
        if (market != null) {
            createOfferDraftWorkflow.setMarket(market);

            createOfferDraftWorkflow.setMarket(market);
            model.getTradePairImage().set(MarketImageComposition.getMarketIcons(market, MARKET_ICON_CACHE));

            MarketListItem item = model.getMarketListItems().stream()
                    .filter(m -> m.getMarket().equals(market))
                    .findAny()
                    .orElse(null);
            model.getSelectedMarketListItem().set(item);

            String baseCurrencyName = market.getBaseCurrencyName();
            model.getHeadlineText().set(Res.get("muSig.offer.create.directionAndMarket.headline",
                    baseCurrencyName, market.getQuoteCurrencyName()));
            model.getBuyButtonText().set(Res.get("muSig.offer.create.directionAndMarket.buyButton", baseCurrencyName));
            model.getSellButtonText().set(Res.get("muSig.offer.create.directionAndMarket.sellButton", baseCurrencyName));
        }
    }

    private void selectDirection(Direction direction) {
        createOfferDraftWorkflow.setDirection(direction);
    }
}
