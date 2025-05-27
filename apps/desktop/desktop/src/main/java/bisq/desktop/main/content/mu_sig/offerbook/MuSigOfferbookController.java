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
import bisq.common.proto.ProtobufUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.mu_sig.create_offer.MuSigCreateOfferController;
import bisq.desktop.main.content.mu_sig.take_offer.MuSigTakeOfferController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.mu_sig.MuSigService;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.CookieKey;
import bisq.settings.FavouriteMarketsService;
import bisq.settings.SettingsService;
import bisq.user.banned.BannedUserService;
import bisq.user.banned.RateLimitExceededException;
import bisq.user.banned.UserProfileBannedException;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

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
    private final Predicate<MarketItem> marketItemsPredicate;
    private final Predicate<MarketItem> favouriteMarketItemsPredicate;
    private Pin offersPin, selectedMarketPin, favouriteMarketsPin, marketPriceByCurrencyMapPin;
    private Subscription selectedMarketItemPin, marketsSearchBoxTextPin, selectedMarketFilterPin, selectedMarketSortTypePin;

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

        marketItemsPredicate = item ->
                model.getMarketFilterPredicate().test(item) &&
                        model.getMarketSearchTextPredicate().test(item) &&
                        model.getMarketPricePredicate().test(item) &&
                        !item.getIsFavourite().get();
        favouriteMarketItemsPredicate = item -> item.getIsFavourite().get();
    }

    @Override
    public void onActivate() {
        List<MarketItem> marketItems = MarketRepository.getAllFiatMarkets().stream()
                .map(market -> new MarketItem(market,
                        favouriteMarketsService,
                        marketPriceService,
                        userProfileService,
                        muSigService))
                .collect(Collectors.toList());
        model.getMarketItems().setAll(marketItems);

        applyInitialSelectedMarket();

        model.getMarketsSearchBoxText().set("");

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

        selectedMarketPin = settingsService.getMuSigSelectedMarket().addObserver(market -> {
            if (market != null) {
                model.getMarketItems().stream()
                        .filter(item -> item.getMarket().equals(market))
                        .findAny()
                        .ifPresent(item -> model.getSelectedMarketItem().set(item));
            }
        });

        favouriteMarketsPin = settingsService.getFavouriteMarkets().addObserver(new CollectionObserver<>() {
            @Override
            public void add(Market market) {
                UIThread.run(() -> {
                    findMarketItem(market).ifPresent(item -> item.getIsFavourite().set(true));
                    updateFilteredMarketItems();
                    updateFavouriteMarketItems();
                });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof Market market) {
                    UIThread.run(() -> {
                        findMarketItem(market).ifPresent(item -> item.getIsFavourite().set(false));
                        updateFilteredMarketItems();
                        updateFavouriteMarketItems();
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> {
                    model.getMarketItems().forEach(item -> item.getIsFavourite().set(false));
                    updateFilteredMarketItems();
                    updateFavouriteMarketItems();
                });
            }
        });

        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() ->
                UIThread.run(() -> {
                    model.setMarketPricePredicate(item -> marketPriceService.getMarketPriceByCurrencyMap().isEmpty() ||
                            marketPriceService.getMarketPriceByCurrencyMap().containsKey(item.getMarket()));
                    updateFilteredMarketItems();
                }));

        selectedMarketItemPin = EasyBind.subscribe(model.getSelectedMarketItem(), selectedMarketItem -> {
            if (selectedMarketItem != null) {
                UIThread.run(() -> {
                    updateFilteredMuSigOfferListItemsPredicate();
                    updateMarketData(selectedMarketItem);
                    updateMarketPrice(selectedMarketItem);
                    settingsService.setMuSigSelectedMarket(selectedMarketItem.getMarket());
                });
            }
        });

        marketsSearchBoxTextPin = EasyBind.subscribe(model.getMarketsSearchBoxText(), searchText -> {
            if (searchText == null || searchText.trim().isEmpty()) {
                model.setMarketSearchTextPredicate(item -> true);
            } else {
                String search = searchText.trim().toLowerCase();
                model.setMarketSearchTextPredicate(item ->
                        item != null &&
                                (item.getMarket().getQuoteCurrencyCode().toLowerCase().contains(search) ||
                                        item.getMarket().getQuoteCurrencyDisplayName().toLowerCase().contains(search))
                );
            }
            updateFilteredMarketItems();
        });

        MarketFilter persistedMarketsFilter = settingsService.getCookie().asString(CookieKey.MU_SIG_MARKETS_FILTER).map(name ->
                ProtobufUtils.enumFromProto(MarketFilter.class, name, MarketFilter.ALL)).orElse(MarketFilter.ALL);
        model.getSelectedMarketsFilter().set(persistedMarketsFilter);
        selectedMarketFilterPin = EasyBind.subscribe(model.getSelectedMarketsFilter(), filter -> {
            if (filter != null) {
                model.setMarketFilterPredicate(MarketFilterPredicate.getPredicate(filter));
                settingsService.setCookie(CookieKey.MU_SIG_MARKETS_FILTER, model.getSelectedMarketsFilter().get().name());
                updateFilteredMarketItems();
            }
            model.getShouldShowAppliedFilters().set(filter == MarketFilter.WITH_OFFERS || filter == MarketFilter.FAVOURITES);
        });

        MarketSortType persistedMarketSortType = settingsService.getCookie().asString(CookieKey.MU_SIG_MARKET_SORT_TYPE).map(name ->
                        ProtobufUtils.enumFromProto(MarketSortType.class, name, MarketSortType.NUM_OFFERS))
                .orElse(MarketSortType.NUM_OFFERS);
        model.getSelectedMarketSortType().set(persistedMarketSortType);
        selectedMarketSortTypePin = EasyBind.subscribe(model.getSelectedMarketSortType(), marketSortType -> {
            if (marketSortType != null) {
                settingsService.setCookie(CookieKey.MU_SIG_MARKET_SORT_TYPE, marketSortType.name());
            }
        });
        model.getSortedMarketItems().setComparator(model.getSelectedMarketSortType().get().getComparator());

        updateFilteredMarketItems();
        updateFavouriteMarketItems();
    }

    @Override
    public void onDeactivate() {
        model.getMuSigOfferListItems().forEach(MuSigOfferListItem::dispose);
        model.getMuSigOfferListItems().clear();
        model.getMuSigOfferIds().clear();

        offersPin.unbind();
        selectedMarketPin.unbind();
        favouriteMarketsPin.unbind();
        marketPriceByCurrencyMapPin.unbind();

        selectedMarketItemPin.unsubscribe();
        marketsSearchBoxTextPin.unsubscribe();
        selectedMarketFilterPin.unsubscribe();
        selectedMarketSortTypePin.unsubscribe();
    }

    void onSelectMarketItem(MarketItem marketItem) {
        if (marketItem == null) {
            model.getSelectedMarketItem().set(null);
            maybeSelectFirst();
        } else {
            model.getSelectedMarketItem().set(marketItem);
            Market market = marketItem.getMarket();
            settingsService.setMuSigSelectedMarket(market);
            settingsService.setCookie(getSelectedMarketCookieKey(), market.getMarketCodes());
        }
    }

    void onCreateOffer() {
        MarketItem marketItem = model.getSelectedMarketItem().get();
        checkArgument(marketItem != null, "No selected market item");
        Navigation.navigateTo(NavigationTarget.MU_SIG_CREATE_OFFER, new MuSigCreateOfferController.InitData(marketItem.getMarket()));
    }

    void onTakeOffer(MuSigOffer offer) {
        Navigation.navigateTo(NavigationTarget.MU_SIG_TAKE_OFFER, new MuSigTakeOfferController.InitData(offer));
    }

    void onRemoveOffer(MuSigOffer muSigOffer) {
        new Popup().warning(Res.get("muSig.offerbook.removeOffer.confirmation"))
                .actionButtonText(Res.get("confirmation.yes"))
                .onAction(() -> doRemoveOffer(muSigOffer))
                .closeButtonText(Res.get("confirmation.no"))
                .show();
    }

    void onSortMarkets(MarketSortType marketSortType) {
        model.getSelectedMarketSortType().set(marketSortType);
        model.getSortedMarketItems().setComparator(marketSortType.getComparator());
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

    private void maybeSelectFirst() {
        MarketItem firstMarketItem = getFirstMarketItem();
        if (firstMarketItem != null) {
            model.getSelectedMarketItem().set(firstMarketItem);
        }
    }

    private MarketItem getFirstMarketItem() {
        return !model.getSortedMarketItems().isEmpty() ? model.getSortedMarketItems().get(0) : null;
    }

    private CookieKey getSelectedMarketCookieKey() {
        // TODO: Update this according to selected base market
        return CookieKey.MU_SIG_OFFERBOOK_SELECTED_BTC_MARKET;
    }

    private void applyInitialSelectedMarket() {
        Optional<Market> selectedMarket = settingsService.getCookie().asString(getSelectedMarketCookieKey())
                .flatMap(MarketRepository::findAnyMarketByMarketCodes)
                .filter(this::isExpectedMarket);

        selectedMarket.flatMap(market -> model.getMarketItems().stream()
                .filter(item -> item.getMarket().equals(market))
                .findAny())
                .ifPresentOrElse(
                        item -> model.getSelectedMarketItem().set(item),
                        () -> model.getSelectedMarketItem().set(getFirstMarketItem())
                );
    }

    private boolean isExpectedMarket(Market market) {
        // TODO: Here we need to use de base market selection instead.
        return market.isBtcFiatMarket() && market.getBaseCurrencyCode().equals("BTC");
    }

    private void updateFilteredMuSigOfferListItemsPredicate() {
        model.getFilteredMuSigOfferListItems().setPredicate(null);
        model.getFilteredMuSigOfferListItems().setPredicate(item ->
            model.getSelectedMarketItem().get().getMarket().equals(item.getMarket()));
    }

    private void updateMarketData(MarketItem selectedMarketItem) {
        if (selectedMarketItem != null) {
            Market selectedMarket = selectedMarketItem.getMarket();
            if (selectedMarket != null) {
                model.getMarketTitle().set(Res.get("muSig.offerbook.marketHeader.title", selectedMarket.getMarketDisplayName()));
                model.getMarketDescription().set(selectedMarket.getMarketCodes());
                marketPriceService
                        .findMarketPrice(selectedMarket)
                        .ifPresentOrElse(
                                marketPrice -> model.getMarketPrice().set(PriceFormatter.format(marketPrice.getPriceQuote(), true)),
                                () -> model.getMarketPrice().set(""));
                model.getBaseCodeTitle().set(selectedMarket.getBaseCurrencyCode());
                model.getQuoteCodeTitle().set(selectedMarket.getQuoteCurrencyCode());
                model.getPriceTitle().set(Res.get("muSig.offerbook.table.header.price", selectedMarket.getMarketCodes()).toUpperCase());
                model.getMarketIconId().set(selectedMarket.getBaseCurrencyCode());
            }
        } else {
            model.getMarketTitle().set("");
            model.getMarketDescription().set("");
            model.getMarketPrice().set("");
        }
    }

    private void updateFilteredMarketItems() {
        model.getFilteredMarketItems().setPredicate(null);
        model.getFilteredMarketItems().setPredicate(marketItemsPredicate);
    }

    private void updateFavouriteMarketItems() {
        // FilteredList has no API for refreshing/invalidating so that the tableView gets updated.
        // Calling refresh on the tableView also did not refresh the collection.
        // Thus, we trigger a change of the predicate to force a refresh.
        model.getFavouriteMarketItems().setPredicate(null);
        model.getFavouriteMarketItems().setPredicate(favouriteMarketItemsPredicate);
        model.getFavouritesListViewNeedsHeightUpdate().set(false);
        model.getFavouritesListViewNeedsHeightUpdate().set(true);
    }

    private Optional<MarketItem> findMarketItem(Market market) {
        return model.getMarketItems().stream()
                .filter(e -> e.getMarket().equals(market))
                .findAny();
    }

    private void updateMarketPrice(MarketItem marketItem) {
        Market selectedMarket = marketItem.getMarket();
        if (selectedMarket != null) {
            marketPriceService
                    .findMarketPrice(selectedMarket)
                    .ifPresent(marketPrice ->
                            model.getMarketPrice().set(PriceFormatter.format(marketPrice.getPriceQuote(), true)));
        }
    }
}
