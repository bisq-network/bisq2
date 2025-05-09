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
import bisq.common.util.StringUtils;
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
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.user.banned.BannedUserService;
import bisq.user.banned.RateLimitExceededException;
import bisq.user.banned.UserProfileBannedException;
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
    private final IdentityService identityService;
    private final BannedUserService bannedUserService;
    protected Pin offersPin;
    private Subscription selectedMarketPin;

    public MuSigOfferbookController(ServiceProvider serviceProvider, Direction direction) {
        muSigService = serviceProvider.getMuSigService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        identityService = serviceProvider.getIdentityService();
        settingsService = serviceProvider.getSettingsService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();

        model = createAndGetModel(direction);
        view = createAndGetView();
    }

    protected abstract V createAndGetView();

    protected abstract M createAndGetModel(Direction direction);

    @Override
    public void onActivate() {
        applyInitialSelectedMarket();
        selectedMarketPin = EasyBind.subscribe(model.getSelectedMarket(), market -> {
            if (market != null) {
                model.getPriceTableHeader().set(Res.get("muSig.offerbook.table.header.price", market.getMarketCodes()).toUpperCase(Locale.ROOT));
                String baseCurrencyCode = market.getBaseCurrencyCode();
                String quoteCurrencyCode = market.getQuoteCurrencyCode();

                if (model.getDirection().isBuy()) {
                    model.getAmountToReceive().set(Res.get("muSig.offerbook.table.header.amountToReceive", baseCurrencyCode).toUpperCase(Locale.ROOT));
                    model.getAmountToSend().set(Res.get("muSig.offerbook.table.header.amountToPay", quoteCurrencyCode).toUpperCase(Locale.ROOT));
                } else {
                    model.getAmountToReceive().set(Res.get("muSig.offerbook.table.header.amountToReceive", quoteCurrencyCode).toUpperCase(Locale.ROOT));
                    model.getAmountToSend().set(Res.get("muSig.offerbook.table.header.amountToSend", baseCurrencyCode).toUpperCase(Locale.ROOT));
                }

                model.setMarketPredicate(item -> item.getOffer().getMarket().equals(market));
                updatePredicate();
            }
        });

        offersPin = muSigService.getObservableOffers().addObserver(new CollectionObserver<>() {
            @Override
            public void add(MuSigOffer muSigOffer) {
                UIThread.run(() -> {
                    String offerId = muSigOffer.getId();
                    if (muSigOffer.getDirection().mirror().equals(model.getDirection()) &&
                            isExpectedMarket(muSigOffer.getMarket()) &&
                            !model.getOfferIds().contains(offerId)) {
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

    protected abstract boolean isExpectedMarket(Market market);

    private void updatePredicate() {
        model.getFilteredList().setPredicate(item -> true);
        model.getFilteredList().setPredicate(item ->
                model.getDirectionPredicate().test(item) &&
                        model.getMarketPredicate().test(item) &&
                        model.getSearchPredicate().test(item)
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
        Navigation.navigateTo(NavigationTarget.MU_SIG_CREATE_OFFER, new MuSigCreateOfferController.InitData(model.getDirection(), model.getSelectedMarket().get()));
    }

    void onTakeOffer(MuSigOffer offer) {
        Navigation.navigateTo(NavigationTarget.MU_SIG_TAKE_OFFER, new MuSigTakeOfferController.InitData(offer));
    }

    void onRemoveOffer(MuSigOffer muSigOffer) {
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


    void onSelectMarket(Market market) {
        settingsService.setSelectedMarket(market);
        settingsService.setCookie(getSelectedMarketCookieKey(), market.getMarketCodes());
    }

    public void onSearchInput(String searchInput) {
        //TODO add a more controlled search impl.
        if (searchInput != null) {
            model.setSearchPredicate(item -> StringUtils.isEmpty(searchInput) ||
                    item.toString().contains(searchInput));
            updatePredicate();
        }
    }

    protected void applyInitialSelectedMarket() {
        Market selectedMarket = settingsService.getCookie().asString(getSelectedMarketCookieKey())
                .flatMap(MarketRepository::findAnyMarketByMarketCodes)
                .filter(this::isExpectedMarket)
                .orElse(getDefaultMarket());
        model.getSelectedMarket().set(selectedMarket);
    }

    protected abstract Market getDefaultMarket();

    protected abstract CookieKey getSelectedMarketCookieKey();
}
