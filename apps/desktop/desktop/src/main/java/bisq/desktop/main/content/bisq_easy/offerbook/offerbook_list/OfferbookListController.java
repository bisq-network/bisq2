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

package bisq.desktop.main.content.bisq_easy.offerbook.offerbook_list;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethodUtil;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.util.ExceptionUtil;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.i18n.Res;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
import javafx.beans.property.ReadOnlyBooleanProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class OfferbookListController implements bisq.desktop.common.view.Controller {
    private final OfferbookListModel model;
    @Getter
    private final OfferbookListView view;
    private final SettingsService settingsService;
    private final UserProfileService userProfileService;
    private final MarketPriceService marketPriceService;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyOfferbookMessageService bisqEasyOfferbookMessageService;
    private Pin showBuyOffersPin, showOfferListExpandedSettingsPin, offerMessagesPin, showMyOffersOnlyPin,
            userIdentityPin, userProfileIdWithScoreChangePin;
    private Subscription showBuyOffersFromModelPin, activeMarketPaymentsCountPin, showMyOffersOnlyFromModelPin;

    public OfferbookListController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        reputationService = serviceProvider.getUserService().getReputationService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        bisqEasyOfferbookMessageService = serviceProvider.getBisqEasyService().getBisqEasyOfferbookMessageService();

        model = new OfferbookListModel();
        view = new OfferbookListView(model, this);
    }

    public ReadOnlyBooleanProperty getShowOfferListExpanded() {
        return model.getShowOfferListExpanded();
    }

    @Override
    public void onActivate() {
        model.setUseAnimations(settingsService.getUseAnimations().get());
        showBuyOffersPin = FxBindings.bindBiDir(model.getShowBuyOffers())
                .to(settingsService.getShowBuyOffers(), settingsService::setShowBuyOffers);
        showOfferListExpandedSettingsPin = FxBindings.bindBiDir(model.getShowOfferListExpanded())
                .to(settingsService.getShowOfferListExpanded(), settingsService::setShowOfferListExpanded);
        showBuyOffersFromModelPin = EasyBind.subscribe(model.getShowBuyOffers(), showBuyOffers -> updatePredicate());
        activeMarketPaymentsCountPin = EasyBind.subscribe(model.getActiveMarketPaymentsCount(), count -> {
            String hint = count.intValue() == 0 ? Res.get("bisqEasy.offerbook.offerList.table.filters.paymentMethods.title.all") : count.toString();
            model.getPaymentFilterTitle().set(Res.get("bisqEasy.offerbook.offerList.table.filters.paymentMethods.title", hint));
            updatePredicate();
        });
        showMyOffersOnlyPin = FxBindings.bindBiDir(model.getShowMyOffersOnly())
                .to(settingsService.getShowMyOffersOnly(), settingsService::setShowMyOffersOnly);
        showMyOffersOnlyFromModelPin = EasyBind.subscribe(model.getShowMyOffersOnly(), showMyOffersOnly -> updatePredicate());
        userIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(userIdentity -> UIThread.run(this::updatePredicate));
        userProfileIdWithScoreChangePin = reputationService.getUserProfileIdWithScoreChange().addObserver(userProfileId -> UIThread.run(this::updatePredicate));
    }

    @Override
    public void onDeactivate() {
        disposeAndClearOfferbookListItems();
        model.getChatMessageIds().clear();

        showBuyOffersPin.unbind();
        showOfferListExpandedSettingsPin.unbind();
        showBuyOffersFromModelPin.unsubscribe();
        activeMarketPaymentsCountPin.unsubscribe();
        if (offerMessagesPin != null) {
            offerMessagesPin.unbind();
        }
        showMyOffersOnlyPin.unbind();
        showMyOffersOnlyFromModelPin.unsubscribe();
        userIdentityPin.unbind();
        userProfileIdWithScoreChangePin.unbind();
    }

    private void disposeAndClearOfferbookListItems() {
        model.getOfferbookListItems().forEach(OfferbookListItem::dispose);
        model.getOfferbookListItems().clear();
    }

    public void setSelectedChannel(BisqEasyOfferbookChannel channel) {
        model.getFiatAmountTitle().set(Res.get("bisqEasy.offerbook.offerList.table.columns.fiatAmount",
                channel.getMarket().getQuoteCurrencyCode()).toUpperCase());
        model.getChannel().set(channel);

        model.getAvailableMarketPayments().setAll(FiatPaymentMethodUtil.getPaymentMethods(channel.getMarket().getQuoteCurrencyCode()));
        applyCookiePaymentFilters();

        if (offerMessagesPin != null) {
            offerMessagesPin.unbind();
        }
        disposeAndClearOfferbookListItems();
        model.getChatMessageIds().clear();
        offerMessagesPin = channel.getChatMessages().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyOfferbookMessage offerbookMessage) {
                if (offerbookMessage.hasBisqEasyOffer() && bisqEasyOfferbookMessageService.isValid(offerbookMessage)) {
                    UIThread.runOnNextRenderFrame(() -> {
                        String messageId = offerbookMessage.getId();
                        if (!model.getChatMessageIds().contains(messageId)) {
                            userProfileService.findUserProfile(offerbookMessage.getAuthorUserProfileId())
                                    .ifPresent(authorUserProfile -> {
                                        OfferbookListItem item = new OfferbookListItem(offerbookMessage,
                                                authorUserProfile,
                                                reputationService,
                                                marketPriceService);
                                        model.getOfferbookListItems().add(item);
                                        model.getChatMessageIds().add(messageId);
                                    });
                        }
                    });
                }
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyOfferbookMessage && ((BisqEasyOfferbookMessage) element).hasBisqEasyOffer()) {
                    UIThread.runOnNextRenderFrame(() -> {
                        BisqEasyOfferbookMessage offerMessage = (BisqEasyOfferbookMessage) element;
                        Optional<OfferbookListItem> toRemove = model.getOfferbookListItems().stream()
                                .filter(item -> item.getBisqEasyOfferbookMessage().getId().equals(offerMessage.getId()))
                                .findAny();
                        toRemove.ifPresent(item -> {
                            item.dispose();
                            model.getOfferbookListItems().remove(item);
                            model.getChatMessageIds().remove(offerMessage.getId());
                        });
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.runOnNextRenderFrame(() -> {
                    disposeAndClearOfferbookListItems();
                    model.getChatMessageIds().clear();
                });
            }
        });
    }

    void toggleOfferList() {
        model.getShowOfferListExpanded().set(!model.getShowOfferListExpanded().get());
    }

    void onSelectOfferMessageItem(OfferbookListItem item) {
        if (item != null) {
            model.getChannel().get().getHighlightedMessage().set(item.getBisqEasyOfferbookMessage());
        }
    }

    void onSelectBuyFromFilter() {
        model.getShowBuyOffers().set(false);
    }

    void onSelectSellToFilter() {
        model.getShowBuyOffers().set(true);
    }

    void togglePaymentFilter(FiatPaymentMethod paymentMethod, boolean isSelected) {
        if (isSelected) {
            model.getSelectedMarketPayments().remove(paymentMethod);
        } else {
            model.getSelectedMarketPayments().add(paymentMethod);
        }
        updateActiveMarketPaymentsCount();
        settingsService.setCookie(CookieKey.BISQ_EASY_OFFER_LIST_PAYMENT_FILTERS, getCookieSubKey(),
                Joiner.on(",").join(model.getSelectedMarketPayments().stream()
                        .map(payment -> payment.getPaymentRail().name()).collect(Collectors.toList())));
    }

    void toggleCustomPaymentFilter(boolean isSelected) {
        boolean newValue = !isSelected;
        model.getIsCustomPaymentsSelected().set(newValue);
        updateActiveMarketPaymentsCount();
        settingsService.setCookie(CookieKey.BISQ_EASY_OFFER_LIST_CUSTOM_PAYMENT_FILTER, getCookieSubKey(), newValue);
    }

    void clearPaymentFilters() {
        model.getSelectedMarketPayments().clear();
        model.getIsCustomPaymentsSelected().set(false);
        updateActiveMarketPaymentsCount();
        settingsService.removeCookie(CookieKey.BISQ_EASY_OFFER_LIST_PAYMENT_FILTERS, getCookieSubKey());
        settingsService.removeCookie(CookieKey.BISQ_EASY_OFFER_LIST_CUSTOM_PAYMENT_FILTER, getCookieSubKey());
    }

    private void applyCookiePaymentFilters() {
        model.getSelectedMarketPayments().clear();
        settingsService.getCookie().asString(CookieKey.BISQ_EASY_OFFER_LIST_PAYMENT_FILTERS, getCookieSubKey())
                .ifPresent(cookie -> {
                    for (String paymentName : Arrays.stream(cookie.split(",")).toList()) {
                        try {
                            FiatPaymentRail persisted = FiatPaymentRail.valueOf(FiatPaymentRail.class, paymentName);
                            model.getSelectedMarketPayments().add(FiatPaymentMethod.fromPaymentRail(persisted));
                        } catch (Exception e) {
                            log.warn("Could not create FiatPaymentRail from persisted name {}. {}", paymentName, ExceptionUtil.getRootCauseMessage(e));
                        }
                    }
                });

        model.getIsCustomPaymentsSelected().set(false);
        settingsService.getCookie().asBoolean(CookieKey.BISQ_EASY_OFFER_LIST_CUSTOM_PAYMENT_FILTER, getCookieSubKey())
                .ifPresent(cookie -> model.getIsCustomPaymentsSelected().set(cookie));

        updateActiveMarketPaymentsCount();
    }

    private void updateActiveMarketPaymentsCount() {
        int count = model.getSelectedMarketPayments().size();
        if (model.getIsCustomPaymentsSelected().get()) {
            ++count;
        }
        model.getActiveMarketPaymentsCount().set(count);
    }

    private void updatePredicate() {
        model.getFilteredOfferbookListItems().setPredicate(item -> {
            if (model.getShowBuyOffers().get() != item.isBuyOffer()) {
                return false;
            }

            boolean paymentFiltersApplied = model.getActiveMarketPaymentsCount().get() != 0;
            if (paymentFiltersApplied) {
                boolean matchesPaymentFilters = item.getFiatPaymentMethods().stream()
                        .anyMatch(payment -> (payment.isCustomPaymentMethod() && model.getIsCustomPaymentsSelected().get())
                                || model.getSelectedMarketPayments().contains(payment));
                if (!matchesPaymentFilters) {
                    return false;
                }
            }

            boolean myOffersOnly = model.getShowMyOffersOnly().get();
            if (myOffersOnly) {
                UserProfile selectedProfile = Optional.ofNullable(userIdentityService.getSelectedUserIdentity())
                        .map(UserIdentity::getUserProfile)
                        .orElse(null);
                boolean isMyOffer = item.getSenderUserProfile().equals(selectedProfile);
                if (!isMyOffer) {
                    return false;
                }
            }

            return true;
        });
    }

    private String getCookieSubKey() {
        return model.getChannel().get().getMarket().getMarketCodes();
    }
}
