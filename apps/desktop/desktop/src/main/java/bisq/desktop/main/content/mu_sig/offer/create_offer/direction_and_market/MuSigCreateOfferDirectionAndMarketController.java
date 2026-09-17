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
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.desktop.main.content.mu_sig.offer.listing.MarketType;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import bisq.offer.Direction;
import bisq.settings.SettingsService;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static bisq.desktop.main.content.mu_sig.offer.create_offer.direction_and_market.MuSigCreateOfferDirectionAndMarketModel.MARKET_ICON_CACHE;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MuSigCreateOfferDirectionAndMarketController implements Controller {
    private final MuSigCreateOfferDirectionAndMarketModel model;
    @Getter
    private final MuSigCreateOfferDirectionAndMarketView view;
    private final Runnable onNextHandler;
    private final MarketPriceService marketPriceService;
    private final MuSigService muSigService;
    private final SettingsService settingsService;
    private Subscription paymentCurrencySearchTextPin, selectedMarketTypePin;

    public MuSigCreateOfferDirectionAndMarketController(ServiceProvider serviceProvider,
                                                        Runnable onNextHandler) {
        this.onNextHandler = onNextHandler;
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        muSigService = serviceProvider.getMuSigService();
        settingsService = serviceProvider.getSettingsService();

        model = new MuSigCreateOfferDirectionAndMarketModel(List.of(new MarketTypeListItem(MarketType.FIAT),
                new MarketTypeListItem(MarketType.OTHER)));
        view = new MuSigCreateOfferDirectionAndMarketView(model, this);
        setDisplayDirection(Direction.BUY);
    }

    public void setMarket(Market market) {
        model.getSelectedMarket().set(market);
        model.getTradePairImage().set(MarketImageComposition.getMarketIcons(market, MARKET_ICON_CACHE));

        model.getSelectedMarketTypeListItem().set(market.isCrypto()
                ? new MarketTypeListItem(MarketType.OTHER)
                : new MarketTypeListItem(MarketType.FIAT));
    }

    public ReadOnlyObjectProperty<Direction> getDisplayDirection() {
        return model.getDisplayDirection();
    }

    public ReadOnlyObjectProperty<Market> getMarket() {
        return model.getSelectedMarket();
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        setDisplayDirection(Direction.BUY);

        model.getPaymentCurrencySearchText().set("");

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
                    .filter(market -> marketPriceService.getMarketPriceByCurrencyMap().containsKey(market))
                    .map(market -> {
                        long numOffersInMarket = muSigService.getOffers().stream()
                                .filter(offer -> offer.getMarket().getRelevantCurrencyCode().equals(market.getRelevantCurrencyCode()))
                                .count();
                        MarketListItem item = new MarketListItem(market, numOffersInMarket);
                        if (market.equals(model.getSelectedMarket().get())) {
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
    public void onDeactivate() {
        paymentCurrencySearchTextPin.unsubscribe();
        selectedMarketTypePin.unsubscribe();
    }

    void onSelectDirection(Direction direction) {
        setDisplayDirection(direction);
        onNextHandler.run();
    }

    void onMarketListItemClicked(@Nullable MarketListItem item) {
        if (item == null || item.equals(model.getSelectedMarketListItem().get())) {
            return;
        }
        setSelectedMarketAndListItem(item.getMarket());
    }

    void onMarketTypeListItemSelected(@Nullable MarketTypeListItem listItem) {
        if (listItem == null || listItem.equals(model.getSelectedMarketTypeListItem().get())) {
            return;
        }
        model.getSelectedMarketTypeListItem().set(listItem);

        updateWithLastSelectedOrDefaultMarket(listItem.getMarketType());
    }

    private void updateWithLastSelectedOrDefaultMarket(MarketType marketType) {
        Optional<Market> lastSelected = switch (marketType) {
            case FIAT -> Optional.ofNullable(settingsService.getMuSigLastSelectedFiatMarket().get());
            case OTHER -> Optional.ofNullable(settingsService.getMuSigLastSelectedOtherMarket().get());
        };

        lastSelected.filter(marketPriceService::hasMarketPrice)
                .or(() -> {
                    Optional<Market> fallback = findFallbackMarket(marketType);
                    log.warn("No market price available for the stored '{}' market, using fallback '{}' market.",
                            lastSelected.orElse(null), fallback.orElse(null));
                    return fallback;
                })
                .ifPresent(this::setSelectedMarketAndListItem);
    }

    private Optional<Market> findFallbackMarket(MarketType marketType) {
        checkArgument(marketType == model.getSelectedMarketTypeListItem().get().getMarketType(),
                "marketType must not be different from the selected market type in the model");
        return model.getMarketListItems().stream().findFirst().map(MarketListItem::getMarket);
    }

    private void setSelectedMarketAndListItem(Market market) {
        updateSelectedMarket(market);
        settingsService.setSelectedMuSigMarket(market);
        if (market.isBtcFiatMarket()) {
            settingsService.setMuSigLastSelectedFiatMarket(market);
        } else {
            settingsService.setMuSigLastSelectedOtherMarket(market);
        }
    }

    private void initializeMarketSelection() {
        Optional<Market> market = Optional.ofNullable(settingsService.getSelectedMuSigMarket().get());
        MarketType marketType = market.map(Market::isBtcFiatMarket)
                .map(fiat -> fiat ? MarketType.FIAT : MarketType.OTHER).orElse(MarketType.FIAT);
        model.getSelectedMarketTypeListItem().set(new MarketTypeListItem(marketType));
        market.filter(marketPriceService::hasMarketPrice)
                .ifPresentOrElse(
                        this::updateSelectedMarket,
                        () -> updateWithLastSelectedOrDefaultMarket(marketType)
                );
    }

    private void updateSelectedMarket(Market market) {
        checkArgument(marketPriceService.hasMarketPrice(market),
                "There is no market price available for the '%s' market", market);

        model.getSelectedMarket().set(market);
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

    private void setDisplayDirection(Direction direction) {
        model.getDisplayDirection().set(direction);
    }
}
