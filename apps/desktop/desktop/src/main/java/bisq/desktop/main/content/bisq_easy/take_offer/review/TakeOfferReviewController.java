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

package bisq.desktop.main.content.bisq_easy.take_offer.review;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.application.DevMode;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.components.PriceInput;
import bisq.desktop.main.content.bisq_easy.components.ReviewDataDisplay;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.FixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// TODO (refactor, low prio) Consider to use a base class to avoid code duplication with TradeWizardReviewController
@Slf4j
public class TakeOfferReviewController implements Controller {
    private final TakeOfferReviewModel model;
    @Getter
    private final TakeOfferReviewView view;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final ChatService chatService;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final PriceInput priceInput;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final BannedUserService bannedUserService;
    private final ReviewDataDisplay reviewDataDisplay;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final MediationRequestService mediationRequestService;
    private Pin errorMessagePin, peersErrorMessagePin;
    private UIScheduler timeoutScheduler;

    public TakeOfferReviewController(ServiceProvider serviceProvider,
                                     Consumer<Boolean> mainButtonsVisibleHandler,
                                     Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        chatService = serviceProvider.getChatService();
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        mediationRequestService = serviceProvider.getSupportService().getMediationRequestService();

        priceInput = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());
        reviewDataDisplay = new ReviewDataDisplay();
        model = new TakeOfferReviewModel();
        view = new TakeOfferReviewView(model, this, reviewDataDisplay.getRoot());
    }

    public void init(BisqEasyOffer bisqEasyOffer) {
        model.setBisqEasyOffer(bisqEasyOffer);
        Market market = bisqEasyOffer.getMarket();
        priceInput.setMarket(market);

        String marketCodes = market.getMarketCodes();
        priceInput.setDescription(Res.get("bisqEasy.takeOffer.review.price.price", marketCodes));

        if (bisqEasyOffer.getAmountSpec() instanceof FixedAmountSpec) {
            OfferAmountUtil.findBaseSideFixedAmount(marketPriceService, bisqEasyOffer)
                    .ifPresent(model::setTakersBaseSideAmount);
            OfferAmountUtil.findQuoteSideFixedAmount(marketPriceService, bisqEasyOffer)
                    .ifPresent(model::setTakersQuoteSideAmount);
        }

        Optional<PriceQuote> priceQuote = PriceUtil.findQuote(marketPriceService, bisqEasyOffer);
        priceQuote.ifPresent(priceInput::setQuote);

        applyPriceQuote(priceQuote);
        applyPriceDetails(bisqEasyOffer.getPriceSpec(), market);
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

    public void setBitcoinPaymentMethodSpec(BitcoinPaymentMethodSpec spec) {
        if (spec != null) {
            model.setBitcoinPaymentMethodSpec(spec);
            model.setBitcoinPaymentMethod(spec.getShortDisplayString());
        }
    }

    public void setFiatPaymentMethodSpec(FiatPaymentMethodSpec spec) {
        if (spec != null) {
            model.setFiatPaymentMethodSpec(spec);
            model.setFiatPaymentMethod(spec.getShortDisplayString());
        }
    }

    public void takeOffer(Runnable onCancelHandler) {
        BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
        if (bannedUserService.isNetworkIdBanned(bisqEasyOffer.getMakerNetworkId())) {
            new Popup().warning(Res.get("bisqEasy.takeOffer.makerBanned.warning")).show();
            onCancelHandler.run();
            return;
        }
        UserIdentity takerIdentity = userIdentityService.getSelectedUserIdentity();
        if (bannedUserService.isUserProfileBanned(takerIdentity.getUserProfile())) {
            // If taker is banned we don't need to show them a popup
            onCancelHandler.run();
            return;
        }
        Optional<UserProfile> mediator = mediationRequestService.selectMediator(bisqEasyOffer.getMakersUserProfileId(),
                takerIdentity.getId(),
                bisqEasyOffer.getId());
        if (!DevMode.isDevMode() && mediator.isEmpty()) {
            new Popup().warning(Res.get("bisqEasy.takeOffer.noMediatorAvailable.warning"))
                    .closeButtonText(Res.get("action.cancel"))
                    .onClose(onCancelHandler)
                    .actionButtonText(Res.get("confirmation.ok"))
                    .onAction(() -> doTakeOffer(bisqEasyOffer, takerIdentity, mediator))
                    .show();
        } else {
            doTakeOffer(bisqEasyOffer, takerIdentity, mediator);
        }
    }

    private void doTakeOffer(BisqEasyOffer bisqEasyOffer, UserIdentity takerIdentity, Optional<UserProfile> mediator) {
        Monetary takersBaseSideAmount = model.getTakersBaseSideAmount();
        Monetary takersQuoteSideAmount = model.getTakersQuoteSideAmount();
        BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec = model.getBitcoinPaymentMethodSpec();
        FiatPaymentMethodSpec fiatPaymentMethodSpec = model.getFiatPaymentMethodSpec();
        PriceSpec priceSpec = bisqEasyOffer.getPriceSpec();
        long marketPrice = model.getMarketPrice();
        BisqEasyProtocol bisqEasyProtocol = bisqEasyTradeService.createBisqEasyProtocol(takerIdentity.getIdentity(),
                bisqEasyOffer,
                takersBaseSideAmount,
                takersQuoteSideAmount,
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                mediator,
                priceSpec,
                marketPrice);
        BisqEasyTrade bisqEasyTrade = bisqEasyProtocol.getModel();
        log.info("Selected mediator for trade {}: {}", bisqEasyTrade.getShortId(), mediator.map(UserProfile::getUserName).orElse("N/A"));
        model.setBisqEasyTrade(bisqEasyTrade);
        errorMessagePin = bisqEasyTrade.errorMessageObservable().addObserver(errorMessage -> {
                    if (errorMessage != null) {
                        UIThread.run(() -> new Popup().error(Res.get("bisqEasy.openTrades.failed.popup",
                                        errorMessage,
                                        StringUtils.truncate(bisqEasyTrade.getErrorStackTrace(), 500)))
                                .show());
                    }
                }
        );
        peersErrorMessagePin = bisqEasyTrade.peersErrorMessageObservable().addObserver(peersErrorMessage -> {
                    if (peersErrorMessage != null) {
                        UIThread.run(() -> new Popup().error(Res.get("bisqEasy.openTrades.failedAtPeer.popup",
                                        peersErrorMessage,
                                        StringUtils.truncate(bisqEasyTrade.getPeersErrorStackTrace(), 500)))
                                .show());
                    }
                }
        );

        bisqEasyTradeService.takeOffer(bisqEasyTrade);
        model.getTakeOfferStatus().set(TakeOfferReviewModel.TakeOfferStatus.SENT);

        BisqEasyContract contract = bisqEasyTrade.getContract();

        mainButtonsVisibleHandler.accept(false);
        String tradeId = bisqEasyTrade.getId();
        if (timeoutScheduler != null) {
            timeoutScheduler.stop();
        }
        timeoutScheduler = UIScheduler.run(() -> {
                    closeAndNavigateToHandler.accept(NavigationTarget.BISQ_EASY);
                    throw new RuntimeException("Take offer message sending did not succeed after 2 minutes.");
                })
                .after(150, TimeUnit.SECONDS); // We have 120 seconds socket timeout, so we should never get triggered here, as the message will be sent as mailbox message
        bisqEasyOpenTradeChannelService.sendTakeOfferMessage(tradeId, bisqEasyOffer, contract.getMediator())
                .thenAccept(result -> UIThread.run(() -> {
                    timeoutScheduler.stop();

                    // In case the user has switched to another market we want to select that market in the offer book
                    ChatChannelSelectionService chatChannelSelectionService =
                            chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OFFERBOOK);
                    bisqEasyOfferbookChannelService.findChannel(contract.getOffer().getMarket())
                            .ifPresent(chatChannelSelectionService::selectChannel);
                    model.getTakeOfferStatus().set(TakeOfferReviewModel.TakeOfferStatus.SUCCESS);
                    bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId)
                            .ifPresent(channel -> {
                                String taker = userIdentityService.getSelectedUserIdentity().getUserProfile().getUserName();
                                String maker = channel.getPeer().getUserName();
                                String encoded = Res.encode("bisqEasy.takeOffer.tradeLogMessage", taker, maker);
                                chatService.getBisqEasyOpenTradeChannelService().sendTradeLogMessage(encoded, channel);
                            });
                }));
    }

    @Override
    public void onActivate() {
        String toSendAmountDescription, toSendAmount, toSendCode, toReceiveAmountDescription, toReceiveAmount, toReceiveCode;
        Monetary fixBaseSideAmount = model.getTakersBaseSideAmount();
        Monetary fixQuoteSideAmount = model.getTakersQuoteSideAmount();
        String formattedBaseAmount = AmountFormatter.formatBaseAmount(fixBaseSideAmount);
        String formattedQuoteAmount = AmountFormatter.formatQuoteAmount(fixQuoteSideAmount);
        Direction takersDirection = model.getBisqEasyOffer().getTakersDirection();
        boolean isMainChain = model.getBitcoinPaymentMethodSpec().getPaymentMethod().getPaymentRail() == BitcoinPaymentRail.MAIN_CHAIN;
        model.setFeeDetailsVisible(isMainChain);
        if (takersDirection.isSell()) {
            toSendAmountDescription = Res.get("bisqEasy.tradeWizard.review.toSend");
            toReceiveAmountDescription = Res.get("bisqEasy.tradeWizard.review.toReceive");
            toSendAmount = formattedBaseAmount;
            toSendCode = fixBaseSideAmount.getCode();
            toReceiveAmount = formattedQuoteAmount;
            toReceiveCode = fixQuoteSideAmount.getCode();

            if (isMainChain) {
                model.setFee(Res.get("bisqEasy.takeOffer.review.sellerPaysMinerFee"));
                model.setFeeDetails(Res.get("bisqEasy.takeOffer.review.noTradeFeesLong"));
            } else {
                model.setFee(Res.get("bisqEasy.takeOffer.review.noTradeFees"));
            }
        } else {
            toSendAmountDescription = Res.get("bisqEasy.tradeWizard.review.toPay");
            toReceiveAmountDescription = Res.get("bisqEasy.tradeWizard.review.toReceive");
            toSendAmount = formattedQuoteAmount;
            toSendCode = fixQuoteSideAmount.getCode();
            toReceiveAmount = formattedBaseAmount;
            toReceiveCode = fixBaseSideAmount.getCode();

            if (isMainChain) {
                model.setFee(Res.get("bisqEasy.takeOffer.review.noTradeFees"));
                model.setFeeDetails(Res.get("bisqEasy.takeOffer.review.sellerPaysMinerFeeLong"));
            } else {
                model.setFee(Res.get("bisqEasy.takeOffer.review.noTradeFees"));
            }
        }

        reviewDataDisplay.setDirection(Res.get("bisqEasy.tradeWizard.review.direction",
                Res.get(takersDirection.isSell() ? "offer.sell" : "offer.buy").toUpperCase()));
        reviewDataDisplay.setToSendAmountDescription(toSendAmountDescription.toUpperCase());
        reviewDataDisplay.setToSendAmount(toSendAmount);
        reviewDataDisplay.setToSendCode(toSendCode);
        reviewDataDisplay.setToReceiveAmountDescription(toReceiveAmountDescription.toUpperCase());
        reviewDataDisplay.setToReceiveAmount(toReceiveAmount);
        reviewDataDisplay.setToReceiveCode(toReceiveCode);
        reviewDataDisplay.setFiatPaymentMethodDescription(Res.get("bisqEasy.tradeWizard.review.paymentMethodDescription.fiat").toUpperCase());
        reviewDataDisplay.setBitcoinPaymentMethod(model.getBitcoinPaymentMethod());
        reviewDataDisplay.setFiatPaymentMethod(model.getFiatPaymentMethod());
    }

    @Override
    public void onDeactivate() {
        if (errorMessagePin != null) {
            errorMessagePin.unbind();
        }
        if (peersErrorMessagePin != null) {
            peersErrorMessagePin.unbind();
        }
        if (timeoutScheduler != null) {
            timeoutScheduler.stop();
        }
    }

    void onShowOpenTrades() {
        closeAndNavigateToHandler.accept(NavigationTarget.BISQ_EASY_OPEN_TRADES);
    }

    private void applyPriceDetails(PriceSpec priceSpec, Market market) {
        Optional<MarketPrice> marketPrice = marketPriceService.findMarketPrice(market);
        marketPrice.ifPresent(price -> model.setMarketPrice(price.getPriceQuote().getValue()));
        Optional<PriceQuote> marketPriceQuote = marketPrice.map(MarketPrice::getPriceQuote);
        String marketPriceAsString = marketPriceQuote.map(PriceFormatter::formatWithCode).orElse(Res.get("data.na"));
        Optional<Double> percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, priceSpec, market);
        double percent = percentFromMarketPrice.orElse(0d);
        if ((priceSpec instanceof FloatPriceSpec || priceSpec instanceof MarketPriceSpec) && percent == 0) {
            model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails", marketPriceAsString));
        } else {
            String aboveOrBelow = percent > 0 ? Res.get("offer.price.above") : Res.get("offer.price.below");
            String percentAsString = percentFromMarketPrice.map(Math::abs).map(PercentageFormatter::formatToPercentWithSymbol)
                    .orElse(Res.get("data.na"));
            if (priceSpec instanceof FloatPriceSpec) {
                model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails.float",
                        percentAsString, aboveOrBelow, marketPriceAsString));
            } else {
                if (percent == 0) {
                    model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails.fix.atMarket",
                            marketPriceAsString));
                } else {
                    model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails.fix",
                            percentAsString, aboveOrBelow, marketPriceAsString));
                }
            }
        }
    }

    private void applyPriceQuote(Optional<PriceQuote> priceQuote) {
        String formattedPrice = priceQuote
                .map(PriceFormatter::format)
                .orElse("");
        String codes = priceQuote.map(e -> e.getMarket().getMarketCodes()).orElse("");
        model.setPrice(Res.get("bisqEasy.tradeWizard.review.price", formattedPrice, codes));
    }
}
