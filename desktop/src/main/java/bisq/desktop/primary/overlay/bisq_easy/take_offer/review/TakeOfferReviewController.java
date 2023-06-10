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

package bisq.desktop.primary.overlay.bisq_easy.take_offer.review;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannelService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.common.currency.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.common.util.MathUtils;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.bisq_easy.components.PriceInput;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.QuoteFormatter;
import bisq.security.KeyPairService;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class TakeOfferReviewController implements Controller {
    private final TakeOfferReviewModel model;
    @Getter
    private final TakeOfferReviewView view;
    private final ReputationService reputationService;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyPublicChatChannelService bisqEasyPublicChatChannelService;
    private final UserProfileService userProfileService;
    private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;
    private final BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService;
    private final MediationService mediationService;
    private final ChatService chatService;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final KeyPairService keyPairService;
    private final PriceInput priceInput;
    private final MarketPriceService marketPriceService;
    private Subscription priceInputPin;

    public TakeOfferReviewController(DefaultApplicationService applicationService, Consumer<Boolean> mainButtonsVisibleHandler) {
        chatService = applicationService.getChatService();
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        bisqEasyPublicChatChannelService = chatService.getBisqEasyPublicChatChannelService();
        bisqEasyChatChannelSelectionService = chatService.getBisqEasyChatChannelSelectionService();
        reputationService = applicationService.getUserService().getReputationService();
        settingsService = applicationService.getSettingsService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        userProfileService = applicationService.getUserService().getUserProfileService();
        bisqEasyPrivateTradeChatChannelService = chatService.getBisqEasyPrivateTradeChatChannelService();
        mediationService = applicationService.getSupportService().getMediationService();
        keyPairService = applicationService.getSecurityService().getKeyPairService();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();

        priceInput = new PriceInput(applicationService.getOracleService().getMarketPriceService());

        model = new TakeOfferReviewModel();
        view = new TakeOfferReviewView(model, this, priceInput.getRoot());
    }

    public void setBisqEasyOffer(BisqEasyOffer bisqEasyOffer) {
        model.setBisqEasyOffer(bisqEasyOffer);
        Market market = bisqEasyOffer.getMarket();
        priceInput.setMarket(market);


        String marketCodes = market.getMarketCodes();
        model.getMarketPriceDescription().set(Res.get("bisqEasy.takeOffer.review.price.marketPrice", marketCodes));
        priceInput.setDescription(Res.get("bisqEasy.takeOffer.review.price.sellersPrice", marketCodes));

        Optional<Quote> sellersQuote = bisqEasyOffer.findQuote(marketPriceService);
        sellersQuote.ifPresentOrElse(priceInput::setQuote,
                () -> log.warn("Cannot set quote at price input as no floatPriceSpec found or no market price available"));
        model.getSellersPriceValue().set(sellersQuote
                .map(QuoteFormatter::formatWithQuoteCode)
                .orElse(Res.get("na")));
        Optional<Quote> marketPriceQuote = marketPriceService.findMarketPrice(market)
                .map(MarketPrice::getQuote);
        String marketPrice = marketPriceQuote
                .map(QuoteFormatter::formatWithQuoteCode)
                .orElse(Res.get("na"));
        Optional<Double> percentFromMarketPrice = bisqEasyOffer.findPercentFromMarketPrice(marketPriceService);
        double percent = percentFromMarketPrice.orElse(0d);
        String details;
        if (percent == 0) {
            details = Res.get("bisqEasy.takeOffer.review.sellersPrice.marketPrice", marketPrice);
        } else {
            String aboveOrBelow = percent > 0 ?
                    Res.get("above") :
                    Res.get("below");
            String percentAsString = percentFromMarketPrice.map(PercentageFormatter::formatToPercentWithSymbol)
                    .orElse(Res.get("na"));
            details = Res.get("bisqEasy.takeOffer.review.sellersPrice.aboveOrBelowMarketPrice",
                    percentAsString, aboveOrBelow, marketPrice);
        }
        model.getSellersPriceValueDetails().set(details);
    }

    public void setSettlementMethodName(String methodName) {
        if (methodName != null) {
            model.getSettlementMethod().set(Res.has(methodName) ? Res.get(methodName) : methodName);

            String direction = model.getBisqEasyOffer().getTakersDirection().isBuy() ? Res.get("buying").toUpperCase() : Res.get("selling").toUpperCase();
            model.getSubtitle().set(Res.get("bisqEasy.takeOffer.review.subtitle", direction, model.getSettlementMethod().get().toUpperCase()));
            model.getMethodValue().set(model.getSettlementMethod().get());
        }
    }

    public void setBaseSideAmount(Monetary monetary) {
        if (monetary != null) {
            model.setBaseSideAmountAsMonetary(monetary);
            model.getBaseSideAmount().set(AmountFormatter.formatAmountWithCode(monetary, false));
            updateAmounts();
        }
    }

    public void doTakeOffer() {
        BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
        Optional<UserProfile> mediator = mediationService.takerSelectMediator(bisqEasyOffer.getMakersUserProfileId());
        bisqEasyPrivateTradeChatChannelService.sendTakeOfferMessage(bisqEasyOffer, mediator)
                .thenAccept(result -> UIThread.run(() -> {
                    ChatChannelSelectionService chatChannelSelectionService = chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY);
                    bisqEasyPrivateTradeChatChannelService.findChannel(bisqEasyOffer)
                            .ifPresent(chatChannelSelectionService::selectChannel);

                    model.getShowTakeOfferSuccess().set(true);
                    mainButtonsVisibleHandler.accept(false);
                }));

        UIScheduler.run(() -> {
            model.getShowTakeOfferSuccess().set(true);
            mainButtonsVisibleHandler.accept(false);
        }).after(1000);
    }

    public void setQuoteSideAmount(Monetary monetary) {
        if (monetary != null) {
            model.setQuoteSideAmountAsMonetary(monetary);
            model.getQuoteSideAmount().set(AmountFormatter.formatAmountWithCode(monetary));
            updateAmounts();
        }
    }

    @Override
    public void onActivate() {
        priceInputPin = EasyBind.subscribe(priceInput.getQuote(), this::onSellerQuoteChanged);
    }

    @Override
    public void onDeactivate() {
        priceInputPin.unsubscribe();
    }


    void onOpenPrivateChat() {
        close();
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_CHAT);
    }

    private void close() {
        OverlayController.hide();
        // If we got started from initial onboarding we are still at Splash screen, so we need to move to main
        Navigation.navigateTo(NavigationTarget.MAIN);
    }

    private void onSellerQuoteChanged(Quote quote) {
        model.getSellersPrice().set(quote != null ? QuoteFormatter.format(quote, true) : "");
        String percentFromMarketPrice = model.getBisqEasyOffer().findPercentFromMarketPrice(marketPriceService)
                .map(percentage -> {
                    return " / " + PercentageFormatter.formatToPercentWithSymbol(percentage);
                })
                .orElse("");

        //  Monetary quoteAmountAsMonetary = model.getBisqEasyOffer().getQuoteAmountAsMonetary();

        model.getSellersPremium().set(Res.get("bisqEasy.takeOffer.review.price.sellersPremiumValue", "12 EUR", "0.0001 BTC", percentFromMarketPrice));
    }

    private void updateAmounts() {
        model.getAmounts().set(model.getQuoteSideAmount().get() + " = " + model.getBaseSideAmount().get());

        Direction takersDirection = model.getBisqEasyOffer().getTakersDirection();
        model.getPayValue().set(takersDirection.isBuy() ? model.getQuoteSideAmount().get() : model.getBaseSideAmount().get());
        model.getReceiveValue().set(takersDirection.isSell() ? model.getQuoteSideAmount().get() : model.getBaseSideAmount().get());

        if (model.getQuoteSideAmountAsMonetary() != null && model.getBaseSideAmountAsMonetary() != null) {
            model.getSellersPremiumValue().set(model.getBisqEasyOffer().findPercentFromMarketPrice(marketPriceService).map(percentage -> {
                long quoteSidePremium = MathUtils.roundDoubleToLong(model.getQuoteSideAmountAsMonetary().getValue() * percentage);
                Monetary quoteSidePremiumAsMonetary = Fiat.fromValue(quoteSidePremium, model.getQuoteSideAmountAsMonetary().getCode());
                long baseSidePremium = MathUtils.roundDoubleToLong(model.getBaseSideAmountAsMonetary().getValue() * percentage);
                Monetary baseSidePremiumAsMonetary = Coin.fromValue(baseSidePremium, model.getBaseSideAmountAsMonetary().getCode());
                return AmountFormatter.formatAmountWithCode(quoteSidePremiumAsMonetary) + " / " +
                        AmountFormatter.formatAmountWithCode(baseSidePremiumAsMonetary, false);
            }).orElse(Res.get("na")));
        }
    }
}
