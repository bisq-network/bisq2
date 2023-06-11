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
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannelService;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.common.currency.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
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
import bisq.offer.amount.AmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.FixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.QuoteFormatter;
import bisq.support.MediationService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class TakeOfferReviewController implements Controller {
    private final TakeOfferReviewModel model;
    @Getter
    private final TakeOfferReviewView view;
    private final BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService;
    private final MediationService mediationService;
    private final ChatService chatService;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final PriceInput priceInput;
    private final MarketPriceService marketPriceService;

    public TakeOfferReviewController(DefaultApplicationService applicationService, Consumer<Boolean> mainButtonsVisibleHandler) {
        chatService = applicationService.getChatService();
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        bisqEasyPrivateTradeChatChannelService = chatService.getBisqEasyPrivateTradeChatChannelService();
        mediationService = applicationService.getSupportService().getMediationService();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();

        priceInput = new PriceInput(applicationService.getOracleService().getMarketPriceService());

        model = new TakeOfferReviewModel();
        view = new TakeOfferReviewView(model, this, priceInput.getRoot());
    }

    public void init(BisqEasyOffer bisqEasyOffer) {
        model.setBisqEasyOffer(bisqEasyOffer);
        Market market = bisqEasyOffer.getMarket();
        priceInput.setMarket(market);

        String marketCodes = market.getMarketCodes();
        priceInput.setDescription(Res.get("bisqEasy.takeOffer.review.price.sellersPrice", marketCodes));

        if (bisqEasyOffer.getAmountSpec() instanceof FixedAmountSpec) {
            model.setTradeAmountSpec(bisqEasyOffer.getAmountSpec());
        }

        if (bisqEasyOffer.getTakersDirection().isBuy()) {
            // If taker is buyer we set the sellers price from the offer
            model.setSellersPriceSpec(bisqEasyOffer.getPriceSpec());

            Optional<PriceQuote> sellersQuote = PriceUtil.findQuote(marketPriceService, bisqEasyOffer);
            sellersQuote.ifPresent(priceInput::setQuote);
            model.getSellersPrice().set(sellersQuote
                    .map(QuoteFormatter::formatWithQuoteCode)
                    .orElse(Res.get("na")));
            applySellersPriceDetails();
        }
    }


    public void setTradePriceSpec(PriceSpec priceSpec) {
        // Only handle if taker is seller
        if (priceSpec != null && model.getBisqEasyOffer() != null && model.getBisqEasyOffer().getTakersDirection().isSell()) {
            model.setSellersPriceSpec(priceSpec);

            Optional<PriceQuote> sellersQuote = PriceUtil.findQuote(marketPriceService, priceSpec, model.getBisqEasyOffer().getMarket());
            sellersQuote.ifPresent(priceInput::setQuote);
            model.getSellersPrice().set(sellersQuote
                    .map(QuoteFormatter::formatWithQuoteCode)
                    .orElse(Res.get("na")));
            applySellersPriceDetails();
        }
    }

    public void setTradeAmountSpec(AmountSpec amountSpec) {
        if (amountSpec != null) {
            model.setTradeAmountSpec(amountSpec);
        }
    }

    public void setPaymentMethodName(String methodName) {
        if (methodName != null) {
            model.getPaymentMethod().set(Res.has(methodName) ? Res.get(methodName) : methodName);

            String direction = model.getBisqEasyOffer().getTakersDirection().isBuy() ? Res.get("buying").toUpperCase() : Res.get("selling").toUpperCase();
            model.getSubtitle().set(Res.get("bisqEasy.takeOffer.review.subtitle", direction, model.getPaymentMethod().get().toUpperCase()));
            model.getMethod().set(model.getPaymentMethod().get());
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


    @Override
    public void onActivate() {
        AmountSpec takersAmountSpec = model.getTradeAmountSpec();
        PriceSpec sellersPriceSpec = model.getSellersPriceSpec();
        Market market = model.getBisqEasyOffer().getMarket();

        Optional<Monetary> quoteAmount = AmountUtil.findQuoteSideFixedAmount(marketPriceService, takersAmountSpec, sellersPriceSpec, market);
        String formattedQuoteAmount = quoteAmount.map(AmountFormatter::formatAmountWithCode).orElse(Res.get("na"));

        Optional<Monetary> baseAmount = AmountUtil.findBaseSideFixedAmount(marketPriceService, takersAmountSpec, sellersPriceSpec, market);
        String formattedBaseAmount = baseAmount.map(AmountFormatter::formatAmountWithCode).orElse(Res.get("na"));
        model.getAmountDescription().set(formattedQuoteAmount + " = " + formattedBaseAmount);

        Direction takersDirection = model.getBisqEasyOffer().getTakersDirection();
        model.getToPay().set(takersDirection.isBuy() ? formattedQuoteAmount : formattedBaseAmount);
        model.getToReceive().set(takersDirection.isSell() ? formattedQuoteAmount : formattedBaseAmount);

        Optional<Double> percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, sellersPriceSpec, market);
        String sellersPremium;
        if (quoteAmount.isPresent() && baseAmount.isPresent() && percentFromMarketPrice.isPresent()) {
            double percentage = percentFromMarketPrice.get();
            long quoteSidePremium = MathUtils.roundDoubleToLong(quoteAmount.get().getValue() * percentage);
            Monetary quoteSidePremiumAsMonetary = Fiat.fromValue(quoteSidePremium, quoteAmount.get().getCode());
            long baseSidePremium = MathUtils.roundDoubleToLong(baseAmount.get().getValue() * percentage);
            Monetary baseSidePremiumAsMonetary = Coin.fromValue(baseSidePremium, baseAmount.get().getCode());
            sellersPremium = AmountFormatter.formatAmountWithCode(quoteSidePremiumAsMonetary) + " / " +
                    AmountFormatter.formatAmountWithCode(baseSidePremiumAsMonetary, false);
        } else {
            sellersPremium = Res.get("na");
        }
        model.getSellersPremium().set(sellersPremium);
    }

    @Override
    public void onDeactivate() {
    }


    void onOpenPrivateChat() {
        close();
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_CHAT);
    }

    private void close() {
        OverlayController.hide();
    }

    private void applySellersPriceDetails() {
        BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
        Market market = bisqEasyOffer.getMarket();
        Optional<PriceQuote> marketPriceQuote = marketPriceService.findMarketPrice(market)
                .map(MarketPrice::getPriceQuote);
        String marketPrice = marketPriceQuote
                .map(QuoteFormatter::formatWithQuoteCode)
                .orElse(Res.get("na"));
        Optional<Double> percentFromMarketPrice;
        percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, model.getSellersPriceSpec(), market);
        double percent = percentFromMarketPrice.orElse(0d);
        String details;
        if (percent == 0) {
            details = Res.get("bisqEasy.takeOffer.review.sellersPrice.marketPrice", marketPrice);
        } else {
            String aboveOrBelow = percent > 0 ?
                    Res.get("above") :
                    Res.get("below");
            String percentAsString = percentFromMarketPrice.map(Math::abs).map(PercentageFormatter::formatToPercentWithSymbol)
                    .orElse(Res.get("na"));
            details = Res.get("bisqEasy.takeOffer.review.sellersPrice.aboveOrBelowMarketPrice",
                    percentAsString, aboveOrBelow, marketPrice);
        }
        model.getSellersPriceDetails().set(details);
    }
}
