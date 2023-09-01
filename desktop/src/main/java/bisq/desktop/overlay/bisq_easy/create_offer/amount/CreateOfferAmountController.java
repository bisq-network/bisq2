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

package bisq.desktop.overlay.bisq_easy.create_offer.amount;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.AmountSpecUtil;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.amount.spec.QuoteSideRangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
public class CreateOfferAmountController implements Controller {
    private final CreateOfferAmountModel model;
    @Getter
    private final CreateOfferAmountView view;
    private final AmountComponent minAmountComponent, maxOrFixAmountComponent;
    private final SettingsService settingsService;
    private final MarketPriceService marketPriceService;
    private final BisqEasyPublicChatChannelService bisqEasyPublicChatChannelService;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;
    private Subscription isMinAmountEnabledPin, maxOrFixAmountCompBaseSideAmountPin, minAmountCompBaseSideAmountPin,
            maxAmountCompQuoteSideAmountPin, minAmountCompQuoteSideAmountPin, areAmountsValidPin;

    public CreateOfferAmountController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        bisqEasyPublicChatChannelService = serviceProvider.getChatService().getBisqEasyPublicChatChannelService();
        model = new CreateOfferAmountModel();

        minAmountComponent = new AmountComponent(serviceProvider, true);
        minAmountComponent.setDescription(Res.get("bisqEasy.createOffer.amount.description.minAmount"));
        maxOrFixAmountComponent = new AmountComponent(serviceProvider, true);

        view = new CreateOfferAmountView(model, this,
                minAmountComponent,
                maxOrFixAmountComponent);
    }

    public void setOpenedFromDashboard(boolean isOpenedFromDashboard) {
        model.setOpenedFromDashboard(isOpenedFromDashboard);
        model.getShowRangeAmounts().set(!isOpenedFromDashboard);
        if (isOpenedFromDashboard) {
            model.getIsMinAmountEnabled().set(false);
        }
    }

    public void setDirection(Direction direction) {
        if (direction == null) {
            return;
        }
        model.setDirection(direction);
        minAmountComponent.setDirection(direction);
        maxOrFixAmountComponent.setDirection(direction);
    }

    public void setMarket(Market market) {
        if (market == null) {
            return;
        }
        minAmountComponent.setMarket(market);
        maxOrFixAmountComponent.setMarket(market);
        model.setMarket(market);
    }

    public void setFiatPaymentMethods(List<FiatPaymentMethod> fiatPaymentMethods) {
        if (fiatPaymentMethods != null) {
            model.setFiatPaymentMethods(fiatPaymentMethods);
        }
    }

    public void setPriceSpec(PriceSpec priceSpec) {
        model.setPriceSpec(priceSpec);
        PriceQuote priceQuote;
        if (priceSpec instanceof FixPriceSpec) {
            priceQuote = ((FixPriceSpec) priceSpec).getPriceQuote();
            minAmountComponent.setQuote(priceQuote);
            maxOrFixAmountComponent.setQuote(priceQuote);
        } else {
            Optional<PriceQuote> marketPriceQuote = getMarketPriceQuote();
            if (marketPriceQuote.isPresent()) {
                if (priceSpec instanceof FloatPriceSpec) {
                    double percentage = ((FloatPriceSpec) priceSpec).getPercentage();
                    priceQuote = PriceUtil.fromMarketPriceMarkup(marketPriceQuote.get(), percentage);
                } else {
                    priceQuote = marketPriceQuote.get();
                }
                minAmountComponent.setQuote(priceQuote);
                maxOrFixAmountComponent.setQuote(priceQuote);
            } else {
                log.error("marketPriceQuote not present");
            }
        }
    }

    public void reset() {
        minAmountComponent.reset();
        maxOrFixAmountComponent.reset();
        model.reset();
    }

    public ReadOnlyObjectProperty<AmountSpec> getAmountSpec() {
        return model.getAmountSpec();
    }

    public ReadOnlyBooleanProperty getIsMinAmountEnabled() {
        return model.getIsMinAmountEnabled();
    }

    public BooleanProperty areAmountsValid() {
        return model.getAreAmountsValid();
    }

    @Override
    public void onActivate() {
        model.setHeadline(model.getDirection().isBuy() ?
                Res.get("bisqEasy.createOffer.amount.headline.buyer") :
                Res.get("bisqEasy.createOffer.amount.headline.seller"));

        Boolean cookieValue = settingsService.getCookie().asBoolean(CookieKey.CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED).orElse(false);
        model.getIsMinAmountEnabled().set(cookieValue && model.getShowRangeAmounts().get());

        minAmountCompBaseSideAmountPin = EasyBind.subscribe(minAmountComponent.getBaseSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get()) {
                        if (value != null && maxOrFixAmountComponent.getBaseSideAmount().get() != null &&
                                value.getValue() > maxOrFixAmountComponent.getBaseSideAmount().get().getValue()) {
                            maxOrFixAmountComponent.setBaseSideAmount(value);
                        }
                    }
                });
        maxOrFixAmountCompBaseSideAmountPin = EasyBind.subscribe(maxOrFixAmountComponent.getBaseSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get() &&
                            value != null && minAmountComponent.getBaseSideAmount().get() != null &&
                            value.getValue() < minAmountComponent.getBaseSideAmount().get().getValue()) {
                        minAmountComponent.setBaseSideAmount(value);
                    }
                });

        minAmountCompQuoteSideAmountPin = EasyBind.subscribe(minAmountComponent.getQuoteSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get()) {
                        if (value != null && maxOrFixAmountComponent.getQuoteSideAmount().get() != null &&
                                value.getValue() > maxOrFixAmountComponent.getQuoteSideAmount().get().getValue()) {
                            maxOrFixAmountComponent.setQuoteSideAmount(value);

                        }
                    }
                    applyAmountSpec();
                });
        maxAmountCompQuoteSideAmountPin = EasyBind.subscribe(maxOrFixAmountComponent.getQuoteSideAmount(),
                value -> {
                    if (model.getIsMinAmountEnabled().get() &&
                            value != null && minAmountComponent.getQuoteSideAmount().get() != null &&
                            value.getValue() < minAmountComponent.getQuoteSideAmount().get().getValue()) {
                        minAmountComponent.setQuoteSideAmount(value);
                    }
                    applyAmountSpec();
                });

        isMinAmountEnabledPin = EasyBind.subscribe(model.getIsMinAmountEnabled(), isMinAmountEnabled -> {
            model.getToggleButtonText().set(isMinAmountEnabled ?
                    Res.get("bisqEasy.createOffer.amount.removeMinAmountOption") :
                    Res.get("bisqEasy.createOffer.amount.addMinAmountOption"));

            maxOrFixAmountComponent.setDescription(isMinAmountEnabled ?
                    Res.get("bisqEasy.createOffer.amount.description.maxAmount") :
                    Res.get("bisqEasy.createOffer.amount.description.fixAmount"));

            applyAmountSpec();
        });

        var binding = EasyBind.combine(
                minAmountComponent.areAmountsValid(),
                maxOrFixAmountComponent.areAmountsValid(),
                model.getIsMinAmountEnabled(),
                (areMinAmountsValid, areMaxOrFixedAmountsValid, isMinAmountEnabled) ->
                        areMaxOrFixedAmountsValid && (!isMinAmountEnabled | areMinAmountsValid)
                );
        areAmountsValidPin = EasyBind.subscribe(binding, areAmountsValid -> model.getAreAmountsValid().set(areAmountsValid));

        applyAmountSpec();

        if (model.getDirection().isSell()) {
            String btcAmount = Res.get("bisqEasy.component.amount.baseSide.tooltip.seller.btcAmount") + "\n";
            maxOrFixAmountComponent.setTooltip(btcAmount + Res.get("bisqEasy.component.amount.baseSide.tooltip.salePrice"));
        } else {
            String btcAmount = Res.get("bisqEasy.component.amount.baseSide.tooltip.buyer.btcAmount") + "\n";
            if (model.isOpenedFromDashboard()) {
                applyBestOfferQuote();
                maxOrFixAmountComponent.setTooltip(model.getBestOffersPrice()
                        .map(bestOffersPrice -> btcAmount + Res.get("bisqEasy.component.amount.baseSide.tooltip.bestOfferPrice", PriceFormatter.formatWithCode(bestOffersPrice)))
                        .orElse(btcAmount + Res.get("bisqEasy.component.amount.baseSide.tooltip.marketPrice")));
            } else {
                maxOrFixAmountComponent.setTooltip(btcAmount + Res.get("bisqEasy.component.amount.baseSide.tooltip.marketPrice"));
            }
        }
    }

    @Override
    public void onDeactivate() {
        isMinAmountEnabledPin.unsubscribe();
        maxOrFixAmountCompBaseSideAmountPin.unsubscribe();
        maxAmountCompQuoteSideAmountPin.unsubscribe();
        minAmountCompBaseSideAmountPin.unsubscribe();
        minAmountCompQuoteSideAmountPin.unsubscribe();
        areAmountsValidPin.unsubscribe();;
    }

    void onToggleMinAmountVisibility() {
        boolean value = !model.getIsMinAmountEnabled().get();
        model.getIsMinAmountEnabled().set(value);
        settingsService.setCookie(CookieKey.CREATE_BISQ_EASY_OFFER_IS_MIN_AMOUNT_ENABLED, value);
    }

    private void applyAmountSpec() {
        if(maxOrFixAmountComponent.getQuoteSideAmount().get() != null) {
            long maxOrFixAmount = maxOrFixAmountComponent.getQuoteSideAmount().get().getValue();

            if (model.getIsMinAmountEnabled().get()) {
                if(minAmountComponent.getQuoteSideAmount().get() != null) {
                    long minAmount = minAmountComponent.getQuoteSideAmount().get().getValue();
                    if (minAmount == maxOrFixAmount) {
                        model.getAmountSpec().set(new QuoteSideFixedAmountSpec(maxOrFixAmount));
                    } else {
                        model.getAmountSpec().set(new QuoteSideRangeAmountSpec(minAmount, maxOrFixAmount));
                    }
                }
            } else {
                model.getAmountSpec().set(new QuoteSideFixedAmountSpec(maxOrFixAmount));
            }
        }
    }

    private void applyBestOfferQuote() {
        Optional<BisqEasyPublicChatChannel> optionalChannel = bisqEasyPublicChatChannelService.findChannel(model.getMarket());
        if (optionalChannel.isPresent() && model.getMarket() != null) {
            Optional<PriceQuote> bestOffersPrice = optionalChannel.get().getChatMessages().stream()
                    .filter(chatMessage -> chatMessage.getBisqEasyOffer().isPresent())
                    .filter(chatMessage -> filterOffers(chatMessage.getBisqEasyOffer().get()))
                    .map(chatMessage -> chatMessage.getBisqEasyOffer().get().getPriceSpec())
                    .flatMap(priceSpec -> PriceUtil.findQuote(marketPriceService, priceSpec, model.getMarket()).or(this::getMarketPriceQuote).stream())
                    .min(Comparator.comparing(PriceQuote::getValue));
            model.setBestOffersPrice(bestOffersPrice);
            if (bestOffersPrice.isPresent()) {
                maxOrFixAmountComponent.setQuote(bestOffersPrice.get());
            } else {
                getMarketPriceQuote().ifPresent(maxOrFixAmountComponent::setQuote);
            }
            AmountSpecUtil.findQuoteSideFixedAmountFromSpec(model.getAmountSpec().get(), model.getMarket().getQuoteCurrencyCode())
                    .ifPresent(amount -> UIThread.runOnNextRenderFrame(() -> maxOrFixAmountComponent.setQuoteSideAmount(amount)));
        }
    }

    private boolean filterOffers(BisqEasyOffer peersOffer) {
        try {
            Optional<UserProfile> authorUserProfile = userProfileService.findUserProfile(peersOffer.getMakersUserProfileId());
            if (authorUserProfile.isEmpty()) {
                return false;
            }
            if (userProfileService.isChatUserIgnored(authorUserProfile.get())) {
                return false;
            }
            if (userIdentityService.getUserIdentities().stream()
                    .map(userIdentity -> userIdentity.getUserProfile().getId())
                    .anyMatch(userProfileId -> userProfileId.equals(authorUserProfile.get().getId()))) {
                return false;
            }

            if (peersOffer.getDirection().equals(model.getDirection())) {
                return false;
            }

            if (!peersOffer.getMarket().equals(model.getMarket())) {
                return false;
            }

            Optional<Monetary> myQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, model.getAmountSpec().get(), model.getPriceSpec(), model.getMarket());
            Optional<Monetary> peersQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, peersOffer);
            if (myQuoteSideMinOrFixedAmount.orElseThrow().getValue() > peersQuoteSideMaxOrFixedAmount.orElseThrow().getValue()) {
                return false;
            }

            Optional<Monetary> myQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, model.getAmountSpec().get(), model.getPriceSpec(), model.getMarket());
            Optional<Monetary> peersQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, peersOffer);
            if (myQuoteSideMaxOrFixedAmount.orElseThrow().getValue() < peersQuoteSideMinOrFixedAmount.orElseThrow().getValue()) {
                return false;
            }

            List<String> paymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(peersOffer.getQuoteSidePaymentMethodSpecs());
            List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = PaymentMethodSpecUtil.createFiatPaymentMethodSpecs(model.getFiatPaymentMethods());
            List<String> quoteSidePaymentMethodNames = PaymentMethodSpecUtil.getPaymentMethodNames(quoteSidePaymentMethodSpecs);
            if (quoteSidePaymentMethodNames.stream().noneMatch(paymentMethodNames::contains)) {
                return false;
            }

            //todo
           /* if (reputationService.getReputationScore(senderUserProfile).getTotalScore() < myChatOffer.getRequiredTotalReputationScore()) {
                return false;
            }*/

            return true;
        } catch (Throwable t) {
            log.error("Error at TakeOfferPredicate", t);
            return false;
        }
    }

    private Optional<PriceQuote> getMarketPriceQuote() {
        return marketPriceService.findMarketPriceQuote(model.getMarket());
    }
}
