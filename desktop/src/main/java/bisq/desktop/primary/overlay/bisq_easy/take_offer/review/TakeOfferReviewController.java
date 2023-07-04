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
import bisq.contract.ContractService;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.bisq_easy.components.PriceInput;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.FixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.oracle.service.market_price.MarketPrice;
import bisq.oracle.service.market_price.MarketPriceService;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.support.mediation.MediationService;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

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
    private final ContractService contractService;
    private final IdentityService identityService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyTradeService bisqEasyTradeService;

    public TakeOfferReviewController(DefaultApplicationService applicationService, Consumer<Boolean> mainButtonsVisibleHandler) {
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        contractService = applicationService.getContractService();
        identityService = applicationService.getIdentityService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        chatService = applicationService.getChatService();
        bisqEasyPrivateTradeChatChannelService = chatService.getBisqEasyPrivateTradeChatChannelService();
        mediationService = applicationService.getSupportService().getMediationService();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        bisqEasyTradeService = applicationService.getTradeService().getBisqEasyTradeService();

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
            OfferAmountUtil.findBaseSideFixedAmount(marketPriceService, bisqEasyOffer)
                    .ifPresent(model::setTakersBaseSideAmount);
            OfferAmountUtil.findQuoteSideFixedAmount(marketPriceService, bisqEasyOffer)
                    .ifPresent(model::setTakersQuoteSideAmount);
        }

        if (bisqEasyOffer.getTakersDirection().isBuy()) {
            // If taker is buyer we set the sellers price from the offer
            model.setSellersPriceSpec(bisqEasyOffer.getPriceSpec());

            Optional<PriceQuote> sellersQuote = PriceUtil.findQuote(marketPriceService, bisqEasyOffer);
            sellersQuote.ifPresent(priceInput::setQuote);
            model.getSellersPrice().set(sellersQuote
                    .map(PriceFormatter::formatWithCode)
                    .orElse(Res.get("data.na")));
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
                    .map(PriceFormatter::formatWithCode)
                    .orElse(Res.get("data.na")));
            applySellersPriceDetails();
        }
    }

    public void setTakersBaseSideAmount(Monetary amount) {
        if (amount != null) {
            model.setTakersBaseSideAmount(amount);
        }
    }

    public void setTakersQuoteSideAmount(Monetary amount) {
        if (amount != null) {
            model.setTakersQuoteSideAmount(amount);
        }
    }

    public void setFiatPaymentMethodSpec(FiatPaymentMethodSpec spec) {
        if (spec != null) {
            model.setFiatPaymentMethodSpec(spec);
            model.getFiatPaymentMethodDisplayString().set(spec.getDisplayString());

            String direction = model.getBisqEasyOffer().getTakersDirection().isBuy() ? Res.get("offer.buying").toUpperCase() : Res.get("offer.selling").toUpperCase();
            model.getSubtitle().set(Res.get("bisqEasy.takeOffer.review.subtitle", direction, model.getFiatPaymentMethodDisplayString().get().toUpperCase()));
            model.getMethod().set(model.getFiatPaymentMethodDisplayString().get());
        }
    }

    public void doTakeOffer() {
        try {
            BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
            UserIdentity myUserIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
            BisqEasyTrade bisqEasyTradeModel = bisqEasyTradeService.onTakeOffer(myUserIdentity.getIdentity(),
                    bisqEasyOffer,
                    model.getTakersBaseSideAmount(),
                    model.getTakersQuoteSideAmount(),
                    bisqEasyOffer.getBaseSidePaymentMethodSpecs().get(0),
                    model.getFiatPaymentMethodSpec());

            model.setBisqEasyTradeModel(bisqEasyTradeModel);

            BisqEasyContract contract = bisqEasyTradeModel.getContract();
            bisqEasyPrivateTradeChatChannelService.sendTakeOfferMessage(bisqEasyOffer, contract.getMediator())
                    .thenAccept(result -> UIThread.run(() -> {
                        ChatChannelSelectionService chatChannelSelectionService = chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY);
                        bisqEasyPrivateTradeChatChannelService.findChannel(bisqEasyOffer)
                                .ifPresent(chatChannelSelectionService::selectChannel);
                        model.getShowTakeOfferSuccess().set(true);
                        mainButtonsVisibleHandler.accept(false);
                    }));
        } catch (TradeException e) {
            new Popup().error(e).show();
        }
    }

    @Override
    public void onActivate() {
        PriceSpec sellersPriceSpec = model.getSellersPriceSpec();
        Market market = model.getBisqEasyOffer().getMarket();

        Monetary baseAmount = model.getTakersBaseSideAmount();
        Monetary quoteAmount = model.getTakersQuoteSideAmount();
        String formattedBaseAmount = AmountFormatter.formatAmountWithCode(baseAmount);
        String formattedQuoteAmount = AmountFormatter.formatAmountWithCode(quoteAmount);
        model.getAmountDescription().set(formattedQuoteAmount + " = " + formattedBaseAmount);

        Direction takersDirection = model.getBisqEasyOffer().getTakersDirection();
        model.getToPay().set(takersDirection.isBuy() ? formattedQuoteAmount : formattedBaseAmount);
        model.getToReceive().set(takersDirection.isSell() ? formattedQuoteAmount : formattedBaseAmount);

        Optional<Double> percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, sellersPriceSpec, market);
        String sellersPremium;
        if (percentFromMarketPrice.isPresent()) {
            double percentage = percentFromMarketPrice.get();
            long quoteSidePremium = MathUtils.roundDoubleToLong(quoteAmount.getValue() * percentage);
            Monetary quoteSidePremiumAsMonetary = Fiat.fromValue(quoteSidePremium, quoteAmount.getCode());
            long baseSidePremium = MathUtils.roundDoubleToLong(baseAmount.getValue() * percentage);
            Monetary baseSidePremiumAsMonetary = Coin.fromValue(baseSidePremium, baseAmount.getCode());
            sellersPremium = AmountFormatter.formatAmountWithCode(quoteSidePremiumAsMonetary) + " / " +
                    AmountFormatter.formatAmountWithCode(baseSidePremiumAsMonetary, false);
        } else {
            sellersPremium = Res.get("data.na");
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
                .map(PriceFormatter::formatWithCode)
                .orElse(Res.get("data.na"));
        Optional<Double> percentFromMarketPrice;
        percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, model.getSellersPriceSpec(), market);
        double percent = percentFromMarketPrice.orElse(0d);
        String details;
        if (percent == 0) {
            details = Res.get("bisqEasy.takeOffer.review.sellersPrice.marketPrice", marketPrice);
        } else {
            String aboveOrBelow = percent > 0 ?
                    Res.get("offer.price.above") :
                    Res.get("offer.price.below");
            String percentAsString = percentFromMarketPrice.map(Math::abs).map(PercentageFormatter::formatToPercentWithSymbol)
                    .orElse(Res.get("data.na"));
            details = Res.get("bisqEasy.takeOffer.review.sellersPrice.aboveOrBelowMarketPrice",
                    percentAsString, aboveOrBelow, marketPrice);
        }
        model.getSellersPriceDetails().set(details);
    }
}
