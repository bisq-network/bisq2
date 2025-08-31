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

import bisq.account.AccountService;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodUtil;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.asset.CryptoAsset;
import bisq.common.asset.CryptoAssetRepository;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.proto.ProtobufUtils;
import bisq.common.util.ExceptionUtil;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.mu_sig.MuSigOfferListItem;
import bisq.desktop.main.content.mu_sig.create_offer.MuSigCreateOfferController;
import bisq.desktop.main.content.mu_sig.take_offer.MuSigTakeOfferController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.mu_sig.MuSigService;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.CookieKey;
import bisq.settings.FavouriteMarketsService;
import bisq.settings.SettingsService;
import bisq.user.banned.BannedUserService;
import bisq.user.banned.RateLimitExceededException;
import bisq.user.banned.UserProfileBannedException;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
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
    private final UserIdentityService userIdentityService;
    private final ReputationService reputationService;
    private final AccountService accountService;
    private Pin offersPin, selectedMarketPin, favouriteMarketsPin, marketPriceByCurrencyMapPin;
    private Subscription selectedMarketItemPin, marketsSearchBoxTextPin, selectedMarketFilterPin, selectedMarketSortTypePin,
            selectedOffersFilterPin, activeMarketPaymentsCountPin, selectedMarketPricePin, selectedBaseCryptoAssetPin;

    public MuSigOfferbookController(ServiceProvider serviceProvider) {
        muSigService = serviceProvider.getMuSigService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        identityService = serviceProvider.getIdentityService();
        settingsService = serviceProvider.getSettingsService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        favouriteMarketsService = serviceProvider.getFavouriteMarketsService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        accountService = serviceProvider.getAccountService();

        model = new MuSigOfferbookModel();
        view = new MuSigOfferbookView(model, this);
    }

    @Override
    public void onActivate() {
        model.getMarketsSearchBoxText().set("");

        offersPin = muSigService.getObservableOffers().addObserver(new CollectionObserver<>() {
            @Override
            public void add(MuSigOffer muSigOffer) {
                UIThread.run(() -> {
                    String offerId = muSigOffer.getId();
                    if (!model.getMuSigOfferIds().contains(offerId)) {
                        model.getMuSigOfferListItems().add(new MuSigOfferListItem(muSigOffer,
                                marketPriceService,
                                userProfileService,
                                identityService,
                                reputationService,
                                accountService));
                        model.getMuSigOfferIds().add(offerId);
                        updateFilteredMuSigOfferListItems();
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
                        updateFilteredMuSigOfferListItems();
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> {
                    model.getMuSigOfferListItems().clear();
                    model.getMuSigOfferIds().clear();
                    updateFilteredMuSigOfferListItems();
                });
            }
        });

        selectedMarketPin = FxBindings.bindBiDir(model.getSelectedMarket())
                .to(settingsService.getSelectedMuSigMarket(), settingsService::setSelectedMuSigMarket);

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

                    if (selectedMarketPricePin != null) {
                        selectedMarketPricePin.unsubscribe();
                    }
                    if (model.getSelectedMarketItem() != null) {
                        selectedMarketPricePin = EasyBind.subscribe(model.getSelectedMarketItem(), selectedMarketItem -> {
                            if (selectedMarketItem != null) {
                                UIThread.run(() -> updateMarketPrice(selectedMarketItem));
                            }
                        });
                    }
                })
        );

        selectedMarketItemPin = EasyBind.subscribe(model.getSelectedMarketItem(), selectedMarketItem -> {
            if (selectedMarketItem != null) {
                UIThread.run(() -> {
                    updateFilteredMuSigOfferListItems();
                    updateMarketData(selectedMarketItem);
                    updateMarketPrice(selectedMarketItem);
                    updateAvailablePaymentMethods();
                    saveMuSigMarketPreferences(selectedMarketItem.getMarket());
                });
            }
        });

        selectedBaseCryptoAssetPin = EasyBind.subscribe(model.getSelectedBaseCryptoAsset(), selectedCrypto -> {
            if (selectedCrypto != null) {
                if (selectedCrypto.equals(CryptoAssetRepository.XMR)) {
                    updateQuoteMarketItems(MarketRepository.getXmrCryptoMarkets());
                    updateSelectedMuSigMarketWithBaseCurrency(selectedCrypto.getCode());
                } else if (selectedCrypto.equals(CryptoAssetRepository.BITCOIN)) {
                    updateQuoteMarketItems(MarketRepository.getAllFiatMarkets());
                    updateSelectedMuSigMarketWithBaseCurrency(selectedCrypto.getCode());
                }
                model.getMarketListTitle().set(getMarketListTitleString(selectedCrypto));
                model.getBaseCurrencyIconId().set(selectedCrypto.getCode());
            } else {
                model.getMarketListTitle().set("");
            }
        });

        marketsSearchBoxTextPin = EasyBind.subscribe(model.getMarketsSearchBoxText(), searchText -> {
            if (searchText == null || searchText.trim().isEmpty()) {
                model.setMarketSearchTextPredicate(item -> true);
            } else {
                String search = searchText.trim().toLowerCase();
                model.setMarketSearchTextPredicate(item -> item != null
                        && (item.getMarket().getQuoteCurrencyCode().toLowerCase().contains(search)
                        || item.getMarket().getQuoteCurrencyDisplayName().toLowerCase().contains(search))
                );
            }
            updateFilteredMarketItems();
        });

        MuSigFilters.MarketFilter persistedMarketsFilter = settingsService.getCookie().asString(CookieKey.MU_SIG_MARKETS_FILTER).map(name ->
                ProtobufUtils.enumFromProto(MuSigFilters.MarketFilter.class, name, MuSigFilters.MarketFilter.ALL)).orElse(MuSigFilters.MarketFilter.ALL);
        model.getSelectedMarketsFilter().set(persistedMarketsFilter);
        selectedMarketFilterPin = EasyBind.subscribe(model.getSelectedMarketsFilter(), filter -> {
            if (filter != null) {
                model.setMarketFilterPredicate(MarketFilterPredicateProvider.getPredicate(filter));
                settingsService.setCookie(CookieKey.MU_SIG_MARKETS_FILTER, model.getSelectedMarketsFilter().get().name());
                updateFilteredMarketItems();
            }
            model.getShouldShowAppliedFilters().set(filter == MuSigFilters.MarketFilter.WITH_OFFERS || filter == MuSigFilters.MarketFilter.FAVOURITES);
        });

        MarketSortType persistedMarketSortType = settingsService.getCookie().asString(CookieKey.MU_SIG_MARKET_SORT_TYPE)
                .map(name -> ProtobufUtils.enumFromProto(MarketSortType.class, name, MarketSortType.NUM_OFFERS))
                .orElse(MarketSortType.NUM_OFFERS);
        model.getSelectedMarketSortType().set(persistedMarketSortType);
        selectedMarketSortTypePin = EasyBind.subscribe(model.getSelectedMarketSortType(), marketSortType -> {
            if (marketSortType != null) {
                settingsService.setCookie(CookieKey.MU_SIG_MARKET_SORT_TYPE, marketSortType.name());
            }
        });
        model.getSortedMarketItems().setComparator(model.getSelectedMarketSortType().get().getComparator());

        MuSigFilters.MuSigOffersFilter persistedOffersFilter = settingsService.getCookie().asString(CookieKey.MU_SIG_OFFERS_FILTER)
                .map(name -> ProtobufUtils.enumFromProto(MuSigFilters.MuSigOffersFilter.class, name, MuSigFilters.MuSigOffersFilter.ALL))
                .orElse(MuSigFilters.MuSigOffersFilter.ALL);
        model.getSelectedMuSigOffersFilter().set(persistedOffersFilter);
        selectedOffersFilterPin = EasyBind.subscribe(model.getSelectedMuSigOffersFilter(), filter -> {
            if (filter != null) {
                if (filter == MuSigFilters.MuSigOffersFilter.ALL) {
                    model.setMuSigOffersFilterPredicate(item -> true);
                } else if (filter == MuSigFilters.MuSigOffersFilter.BUY) {
                    model.setMuSigOffersFilterPredicate(item -> item.getDirection() == Direction.BUY);
                } else if (filter == MuSigFilters.MuSigOffersFilter.SELL) {
                    model.setMuSigOffersFilterPredicate(item -> item.getDirection() == Direction.SELL);
                } else if (filter == MuSigFilters.MuSigOffersFilter.MINE) {
                    Set<String> myUserProfileIds = userIdentityService.getMyUserProfileIds();
                    model.setMuSigOffersFilterPredicate(item -> myUserProfileIds.contains(item.getMakerUserProfile().getId()));
                }
                settingsService.setCookie(CookieKey.MU_SIG_OFFERS_FILTER, filter.name());
                updateFilteredMuSigOfferListItems();
            }
        });

        activeMarketPaymentsCountPin = EasyBind.subscribe(model.getActiveMarketPaymentsCount(), count -> {
            if (count != null) {
                updatePaymentFilterTitle(count.intValue());
                updatePaymentFilterPredicate();
            }
        });

        updateFilteredMarketItems();
        updateFavouriteMarketItems();
        updateFilteredMuSigOfferListItems();
        selectBaseCurrency();
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
        selectedBaseCryptoAssetPin.unsubscribe();
        marketsSearchBoxTextPin.unsubscribe();
        selectedMarketFilterPin.unsubscribe();
        selectedMarketSortTypePin.unsubscribe();
        selectedOffersFilterPin.unsubscribe();
        activeMarketPaymentsCountPin.unsubscribe();
        if (selectedMarketPricePin != null) {
            selectedMarketPricePin.unsubscribe();
        }
    }

    void onSelectMarketItem(MarketItem marketItem) {
        if (marketItem == null) {
            model.getSelectedMarketItem().set(null);
            maybeSelectFirst();
        } else {
            model.getSelectedMarketItem().set(marketItem);
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

    void onHandleCannotTakeOfferCase(String cannotTakeOfferReason) {
        new Popup().warning(cannotTakeOfferReason)
                .actionButtonText(Res.get("confirmation.yes"))
                .onAction(() -> Navigation.navigateTo(NavigationTarget.FIAT_PAYMENT_ACCOUNTS))
                .closeButtonText(Res.get("confirmation.no"))
                .show();
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

    void onTogglePaymentFilter(FiatPaymentMethod paymentMethod, boolean isSelected) {
        if (isSelected) {
            model.getSelectedPaymentMethods().remove(paymentMethod);
        } else {
            model.getSelectedPaymentMethods().add(paymentMethod);
        }
        updateActiveMarketPaymentsCount();
        settingsService.setCookie(CookieKey.MU_SIG_OFFER_PAYMENT_FILTERS, getCookieSubKey(),
                Joiner.on(",").join(model.getSelectedPaymentMethods().stream()
                        .map(payment -> payment.getPaymentRail().name()).collect(Collectors.toList())));
    }

    void onClearPaymentFilters() {
        model.getSelectedPaymentMethods().clear();
        updateActiveMarketPaymentsCount();
        settingsService.removeCookie(CookieKey.MU_SIG_OFFER_PAYMENT_FILTERS, getCookieSubKey());
    }

    void updateSelectedBaseCryptoAsset(CryptoAsset baseCrypto) {
        UIThread.run(() -> model.getSelectedBaseCryptoAsset().set(baseCrypto));
    }

    private void updateSelectedMuSigMarketWithBaseCurrency(String baseCurrencyCode) {
        Market market = Optional.ofNullable(settingsService.getMuSigLastSelectedMarketByBaseCurrencyMap().get(baseCurrencyCode))
                .orElseGet(() -> {
                    if (baseCurrencyCode.equals(CryptoAssetRepository.XMR.getCode())) {
                        return MarketRepository.getXmrCryptoMarkets().get(0);
                    } else if (baseCurrencyCode.equals(CryptoAssetRepository.BITCOIN.getCode())) {
                        return MarketRepository.getDefaultBtcFiatMarket();
                    }
                    return null;
                });
        findMarketItem(market).ifPresent(item -> model.getSelectedMarketItem().set(item));
    }

    private void updateQuoteMarketItems(List<Market> availableMarkets) {
        List<MarketItem> marketItems = availableMarkets.stream()
                .map(market -> new MarketItem(market,
                        favouriteMarketsService,
                        marketPriceService,
                        userProfileService,
                        muSigService))
                .collect(Collectors.toList());
        model.getMarketItems().setAll(marketItems);
    }

    private void updateActiveMarketPaymentsCount() {
        model.getActiveMarketPaymentsCount().set(model.getSelectedPaymentMethods().size());
    }

    private String getCookieSubKey() {
        return model.getSelectedMarketItem().get().getMarket().getMarketCodes();
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

    private void updateFilteredMuSigOfferListItems() {
        model.getFilteredMuSigOfferListItems().setPredicate(null);
        model.getFilteredMuSigOfferListItems().setPredicate(model.getMuSigOfferListItemsPredicate());
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
                model.getBaseCodeTitle().set(Res.get("muSig.offerbook.table.header.amount", selectedMarket.getBaseCurrencyCode()).toUpperCase());
                model.getQuoteCodeTitle().set(Res.get("muSig.offerbook.table.header.amount", selectedMarket.getQuoteCurrencyCode()).toUpperCase());
                model.getPriceTitle().set(Res.get("muSig.offerbook.table.header.price", selectedMarket.getMarketCodes()).toUpperCase());
                model.getQuoteCurrencyIconId().set(selectedMarket.getQuoteCurrencyCode());
            }
        } else {
            model.getMarketTitle().set("");
            model.getMarketDescription().set("");
            model.getMarketPrice().set("");
        }
    }

    private String getMarketListTitleString(CryptoAsset cryptoAsset) {
        String key = "muSig.offerbook.marketListTitle." + cryptoAsset.getCode().toLowerCase();
        return Res.get(key);
    }

    private void updateFilteredMarketItems() {
        model.getFilteredMarketItems().setPredicate(null);
        model.getFilteredMarketItems().setPredicate(model.getMarketItemsPredicate());
    }

    private void updateFavouriteMarketItems() {
        // FilteredList has no API for refreshing/invalidating so that the tableView gets updated.
        // Calling refresh on the tableView also did not refresh the collection.
        // Thus, we trigger a change of the predicate to force a refresh.
        model.getFavouriteMarketItems().setPredicate(null);
        model.getFavouriteMarketItems().setPredicate(model.getFavouriteMarketItemsPredicate());
        model.getFavouritesListViewNeedsHeightUpdate().set(false);
        model.getFavouritesListViewNeedsHeightUpdate().set(true);
        model.getShouldShowFavouritesListView().set(!model.getFavouriteMarketItems().isEmpty());
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

    private void updateAvailablePaymentMethods() {
        model.getAvailablePaymentMethods().setAll(
                FiatPaymentMethodUtil.getPaymentMethods(model.getSelectedMarketItem().get().getMarket().getQuoteCurrencyCode()));
        applyCookiePaymentFilters();
    }

    private void applyCookiePaymentFilters() {
        model.getSelectedPaymentMethods().clear();
        settingsService.getCookie().asString(CookieKey.MU_SIG_OFFER_PAYMENT_FILTERS, getCookieSubKey())
                .ifPresent(cookie -> {
                    for (String paymentName : Arrays.stream(cookie.split(",")).toList()) {
                        try {
                            FiatPaymentRail persisted = FiatPaymentRail.valueOf(FiatPaymentRail.class, paymentName);
                            model.getSelectedPaymentMethods().add(FiatPaymentMethod.fromPaymentRail(persisted));
                        } catch (Exception e) {
                            log.warn("Could not create FiatPaymentRail from persisted name {}. {}", paymentName, ExceptionUtil.getRootCauseMessage(e));
                        }
                    }
                });

        updateActiveMarketPaymentsCount();
    }

    private void updatePaymentFilterPredicate() {
        boolean paymentFiltersApplied = model.getActiveMarketPaymentsCount().get() != 0;
        if (paymentFiltersApplied) {
            model.setPaymentMethodFilterPredicate(item ->
                    item.getPaymentMethods().stream()
                            .anyMatch(payment -> model.getSelectedPaymentMethods().contains(payment)));
        } else {
            model.setPaymentMethodFilterPredicate(item -> true);
        }
        updateFilteredMuSigOfferListItems();
    }

    private void updatePaymentFilterTitle(int count) {
        String hint = count == 0 ? Res.get("muSig.offerbook.offerListSubheader.paymentMethods.all") : String.valueOf(count);
        model.getPaymentFilterTitle().set(Res.get("muSig.offerbook.offerListSubheader.paymentMethods", hint));
    }

    private void saveMuSigMarketPreferences(Market market) {
        if (market != null) {
            model.getSelectedMarket().set(market);
            settingsService.setMuSigLastSelectedMarketByBaseCurrencyMap(market);
        }
    }

    private void selectBaseCurrency() {
        CryptoAsset cryptoAsset = Optional.ofNullable(model.getSelectedMarket().get())
                .flatMap(market -> CryptoAssetRepository.find(market.getBaseCurrencyCode()))
                .orElse(CryptoAssetRepository.BITCOIN);
        model.getSelectedBaseCryptoAsset().set(cryptoAsset);
    }
}
