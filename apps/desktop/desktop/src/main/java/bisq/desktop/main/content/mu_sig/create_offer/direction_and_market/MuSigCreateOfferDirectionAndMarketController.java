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

package bisq.desktop.main.content.mu_sig.create_offer.direction_and_market;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.asset.CryptoAsset;
import bisq.common.asset.CryptoAssetRepository;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.mu_sig.MuSigService;
import bisq.offer.Direction;
import bisq.settings.SettingsService;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MuSigCreateOfferDirectionAndMarketController implements Controller {
    private final static List<CryptoAsset> baseCryptoAssets = List.of(CryptoAssetRepository.BITCOIN, CryptoAssetRepository.XMR);

    private final MuSigCreateOfferDirectionAndMarketModel model;
    @Getter
    private final MuSigCreateOfferDirectionAndMarketView view;
    private final Runnable onNextHandler;
    private final MarketPriceService marketPriceService;
    private final MuSigService muSigService;
    private final SettingsService settingsService;
    private Subscription searchTextPin, selectedBaseCryptoAssetPin;

    public MuSigCreateOfferDirectionAndMarketController(ServiceProvider serviceProvider,
                                                        Runnable onNextHandler) {
        this.onNextHandler = onNextHandler;
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        muSigService = serviceProvider.getMuSigService();
        settingsService = serviceProvider.getSettingsService();

        model = new MuSigCreateOfferDirectionAndMarketModel();
        view = new MuSigCreateOfferDirectionAndMarketView(model, this);
        setDirection(Direction.BUY);
    }

    public void setMarket(Market market) {
        model.getSelectedMarket().set(market);
    }

    public ReadOnlyObjectProperty<Direction> getDirection() {
        return model.getDirection();
    }

    public ReadOnlyObjectProperty<Market> getMarket() {
        return model.getSelectedMarket();
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        setDirection(Direction.BUY);

        model.getSearchText().set("");

        selectedBaseCryptoAssetPin = EasyBind.subscribe(model.getSelectedBaseCryptoAsset(), cryptoAsset -> {
            if (cryptoAsset == null) {
                return;
            }

            List<Market> markets;
            if (cryptoAsset.equals(CryptoAssetRepository.XMR)) {
                markets = MarketRepository.getXmrCryptoMarkets();
            } else if (cryptoAsset.equals(CryptoAssetRepository.BITCOIN)) {
                markets = MarketRepository.getAllFiatMarkets();
            } else {
                log.warn("Unsupported base crypto asset selected: {}", cryptoAsset);
                markets = List.of();
            }
            model.getMarketListItems().setAll(markets.stream().filter(market ->
                    marketPriceService.getMarketPriceByCurrencyMap().containsKey(market)).map(market -> {
                long numOffersInMarket = muSigService.getOffers().stream().filter(offer -> {
                    Market offerMarket = offer.getMarket();
                    boolean isBaseMarket = offerMarket.getBaseCurrencyCode().equals(model.getSelectedBaseCryptoAsset().get().getCode());
                    boolean isQuoteMarket = offerMarket.getQuoteCurrencyCode().equals(market.getQuoteCurrencyCode());
                    return isBaseMarket && isQuoteMarket;
                }).count();
                MuSigCreateOfferDirectionAndMarketView.MarketListItem item =
                        new MuSigCreateOfferDirectionAndMarketView.MarketListItem(market, numOffersInMarket);
                if (market.equals(model.getSelectedMarket().get())) {
                    model.getSelectedMarketListItem().set(item);
                }
                return item;
            }).collect(Collectors.toList()));
        });

        model.getBaseCryptoAssetListItems().setAll(baseCryptoAssets.stream().map(cryptoAsset -> {
            long numOffersInCryptoAsset = muSigService.getOffers().stream()
                    .filter(offer -> offer.getMarket().getBaseCurrencyCode().equals(cryptoAsset.getCode())).count();
            MuSigCreateOfferDirectionAndMarketView.BaseCryptoAssetListItem item =
                    new MuSigCreateOfferDirectionAndMarketView.BaseCryptoAssetListItem(cryptoAsset, numOffersInCryptoAsset);
            if (cryptoAsset.equals(model.getSelectedBaseCryptoAsset().get())) {
                model.getSelectedBaseCryptoAssetListItem().set(item);
            }
            return item;
        }).collect(Collectors.toList()));

        searchTextPin = EasyBind.subscribe(model.getSearchText(), searchText -> {
            if (searchText == null || searchText.isEmpty()) {
                model.getFilteredMarketListItems().setPredicate(item -> true);
            } else {
                String search = searchText.toLowerCase();
                model.getFilteredMarketListItems().setPredicate(item ->
                        item != null &&
                                (item.getQuoteCurrencyDisplayName().toLowerCase().contains(search) ||
                                        item.getMarket().getQuoteCurrencyDisplayName().toLowerCase().contains(search))
                );
            }
        });

        initializeMarketSelection();
    }

    @Override
    public void onDeactivate() {
        selectedBaseCryptoAssetPin.unsubscribe();
        searchTextPin.unsubscribe();
    }

    void onSelectDirection(Direction direction) {
        setDirection(direction);
        onNextHandler.run();
    }

    void onMarketListItemClicked(MuSigCreateOfferDirectionAndMarketView.MarketListItem item) {
        if (item == null || item.equals(model.getSelectedMarketListItem().get())) {
            return;
        }
        setSelectedMarketAndListItem(item.getMarket());
    }

    void onBaseCryptoAssetListItemClicked(MuSigCreateOfferDirectionAndMarketView.BaseCryptoAssetListItem item) {
        if (item == null || item.equals(model.getSelectedBaseCryptoAssetListItem().get())) {
            return;
        }
        model.getSelectedBaseCryptoAssetListItem().set(item);
        model.getSelectedBaseCryptoAsset().set(item.getCryptoAsset());

        updateWithLastSelectedOrDefaultMarket(item.getCryptoAsset().getCode());
    }

    private void updateWithLastSelectedOrDefaultMarket(String code) {
        if (settingsService.getMuSigLastSelectedMarketByBaseCurrencyMap().containsKey(code)) {
            setSelectedMarketAndListItem(settingsService.getMuSigLastSelectedMarketByBaseCurrencyMap().get(code));
        } else {
            setDefaultMarketForBaseCryptoAsset(code);
        }
    }

    private void setDefaultMarketForBaseCryptoAsset(String baseCurrencyCode) {
        if (baseCurrencyCode.equals(CryptoAssetRepository.XMR.getCode())) {
            Market defaultXmrMarket = MarketRepository.getXmrCryptoMarkets().get(0);
            setSelectedMarketAndListItem(defaultXmrMarket);
        } else if (baseCurrencyCode.equals(CryptoAssetRepository.BITCOIN.getCode())) {
            Market defaultBtcMarket = MarketRepository.getDefaultBtcFiatMarket();
            setSelectedMarketAndListItem(defaultBtcMarket);
        }
    }

    private void setSelectedMarketAndListItem(Market market) {
        MuSigCreateOfferDirectionAndMarketView.MarketListItem item = model.getMarketListItems().stream()
                .filter(m -> m.getMarket().equals(market))
                .findAny().orElse(null);
        model.getSelectedMarketListItem().set(item);
        model.getSelectedMarket().set(market);
        settingsService.setSelectedMuSigMarket(market);
        settingsService.setMuSigLastSelectedMarketByBaseCurrencyMap(market);
    }

    private void initializeMarketSelection() {
        Market market = settingsService.getSelectedMuSigMarket().get();
        if (market != null) {
            model.getSelectedMarket().set(market);
            MuSigCreateOfferDirectionAndMarketView.MarketListItem item = model.getMarketListItems().stream()
                    .filter(m -> m.getMarket().equals(market))
                    .findAny().orElse(null);
            model.getSelectedMarketListItem().set(item);
            model.getSelectedBaseCryptoAsset().set(
                    baseCryptoAssets.stream()
                            .filter(cryptoAsset -> cryptoAsset.getCode().equals(market.getBaseCurrencyCode()))
                            .findAny().orElse(CryptoAssetRepository.BITCOIN)
            );
            model.getSelectedBaseCryptoAssetListItem().set(
                    model.getBaseCryptoAssetListItems().stream()
                            .filter(i -> i.getCryptoAsset().equals(model.getSelectedBaseCryptoAsset().get()))
                            .findAny().orElse(null)
            );
        } else {
            updateWithLastSelectedOrDefaultMarket(CryptoAssetRepository.BITCOIN.getCode());
        }
    }

    private void setDirection(Direction direction) {
        model.getDirection().set(direction);
    }
}
