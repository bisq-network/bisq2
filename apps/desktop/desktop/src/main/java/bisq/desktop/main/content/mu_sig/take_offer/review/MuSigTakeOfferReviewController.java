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

package bisq.desktop.main.content.mu_sig.take_offer.review;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.bonded_roles.market_price.NoMarketPriceAvailableException;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.components.PriceInput;
import bisq.desktop.main.content.bisq_easy.components.ReviewDataDisplay;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import bisq.network.NetworkService;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.FixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
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
import bisq.support.mediation.NoMediatorAvailableException;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import bisq.user.banned.BannedUserService;
import bisq.user.banned.RateLimitExceededException;
import bisq.user.banned.UserProfileBannedException;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class MuSigTakeOfferReviewController implements Controller {
    private final MuSigTakeOfferReviewModel model;
    @Getter
    private final MuSigTakeOfferReviewView view;
    private final ChatService chatService;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final PriceInput priceInput;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final BannedUserService bannedUserService;
    private final ReviewDataDisplay reviewDataDisplay;
    private final MediationRequestService mediationRequestService;
    private final MuSigService muSigService;
    private final NetworkService networkService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private Pin errorMessagePin, peersErrorMessagePin;
    private UIScheduler timeoutScheduler;

    public MuSigTakeOfferReviewController(ServiceProvider serviceProvider,
                                          Consumer<Boolean> mainButtonsVisibleHandler,
                                          Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        chatService = serviceProvider.getChatService();
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        muSigService = serviceProvider.getMuSigService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        mediationRequestService = serviceProvider.getSupportService().getMediationRequestService();
        networkService = serviceProvider.getNetworkService();
        muSigOpenTradeChannelService = serviceProvider.getChatService().getMuSigOpenTradeChannelService();

        priceInput = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());
        reviewDataDisplay = new ReviewDataDisplay();
        model = new MuSigTakeOfferReviewModel();
        view = new MuSigTakeOfferReviewView(model, this, reviewDataDisplay.getRoot());
    }

    public void init(MuSigOffer bisqEasyOffer) {
        model.setMuSigOffer(bisqEasyOffer);
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
        MuSigOffer muSigOffer = model.getMuSigOffer();
        Monetary takersBaseSideAmount = model.getTakersBaseSideAmount();
        Monetary takersQuoteSideAmount = model.getTakersQuoteSideAmount();
        FiatPaymentMethodSpec fiatPaymentMethodSpec = model.getFiatPaymentMethodSpec();
        BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec = model.getBitcoinPaymentMethodSpec();

        mainButtonsVisibleHandler.accept(false);

        try {
            UserIdentity takerIdentity = userIdentityService.getSelectedUserIdentity();
            MuSigProtocol muSigProtocol = muSigService.createProtocol(takerIdentity,
                    muSigOffer,
                    takersBaseSideAmount,
                    takersQuoteSideAmount,
                    bitcoinPaymentMethodSpec,
                    fiatPaymentMethodSpec);
            MuSigTrade muSigTrade = muSigProtocol.getTrade();
            model.setMuSigTrade(muSigTrade);
            MuSigOpenTradeChannel channel = muSigService.createsMuSigOpenTradeChannel(muSigTrade, takerIdentity);

            if (timeoutScheduler != null) {
                timeoutScheduler.stop();
            }
            timeoutScheduler = UIScheduler.run(() -> {
                        closeAndNavigateToHandler.accept(NavigationTarget.MU_SIG);
                        new Popup().warning(Res.get("muSig.takeOffer.timeout.warning", 150)).show();
                    })
                    .after(150, TimeUnit.SECONDS);
            // We have 120 seconds socket timeout, so we should never
            // get triggered here, as the message will be sent as mailbox message

            errorMessagePin = muSigTrade.errorMessageObservable().addObserver(errorMessage -> {
                        if (errorMessage != null) {
                            UIThread.run(() -> new Popup().error(Res.get("bisqEasy.openTrades.failed.popup",
                                            errorMessage,
                                            StringUtils.truncate(muSigTrade.getErrorStackTrace(), 2000)))
                                    .show());
                        }
                    }
            );
            peersErrorMessagePin = muSigTrade.peersErrorMessageObservable().addObserver(peersErrorMessage -> {
                        if (peersErrorMessage != null) {
                            UIThread.run(() -> new Popup().error(Res.get("bisqEasy.openTrades.failedAtPeer.popup",
                                            peersErrorMessage,
                                            StringUtils.truncate(muSigTrade.getPeersErrorStackTrace(), 2000)))
                                    .show());
                        }
                    }
            );

            // Start the protocol
            muSigService.takeOffer(muSigTrade);

            // todo We send the protocol message and log message inside the protocol handler and don't have an easy way
            //  to get notified about the delivery state.
            model.getTakeOfferStatus().set(MuSigTakeOfferReviewModel.TakeOfferStatus.SENT);
            // todo simulate a small delay until we have a solution for the above issue
            UIScheduler.run(() -> model.getTakeOfferStatus().set(MuSigTakeOfferReviewModel.TakeOfferStatus.SUCCESS)).after(200);
        } catch (UserProfileBannedException e) {
            UIThread.run(() -> {
                if (muSigOffer.getMakersUserProfileId().equals(e.getUserProfileId())) {
                    new Popup().warning(Res.get("muSig.takeOffer.banned.maker.warning")).show();
                } else {
                    // We do not inform banned users about being banned
                    log.debug("Takers user profile was banned");
                }
                onCancelHandler.run();
            });
        } catch (RateLimitExceededException e) {
            UIThread.run(() -> {
                if (muSigOffer.getMakersUserProfileId().equals(e.getUserProfileId())) {
                    new Popup().warning(Res.get("muSig.takeOffer.rateLimitsExceeded.maker.warning")).show();
                } else {
                    String exceedsLimitInfo = bannedUserService.getExceedsLimitInfo(e.getUserProfileId()).orElse(Res.get("data.na"));
                    new Popup().warning(Res.get("muSig.takeOffer.rateLimitsExceeded.taker.warning", exceedsLimitInfo)).show();
                }
                onCancelHandler.run();
            });
        } catch (NoMediatorAvailableException e) {
            UIThread.run(() -> new Popup().warning(Res.get("bisqEasy.takeOffer.noMediatorAvailable.warning"))
                    .closeButtonText(Res.get("action.cancel"))
                    .onClose(onCancelHandler)
                    .actionButtonText(Res.get("confirmation.ok"))
                    .onAction(() -> {
                        try {
                            //todo
                           /* muSigService.takeOffer(muSigOffer,
                                    takersBaseSideAmount,
                                    takersQuoteSideAmount,
                                    bitcoinPaymentMethodSpec,
                                    fiatPaymentMethodSpec,
                                    false
                            );*/
                        } catch (Exception ignore) {
                        }
                    })
                    .show());
        } catch (NoMarketPriceAvailableException e) {
            UIThread.run(() -> new Popup().warning(e.getMessage()).show());
        }
    }


    @Override
    public void onActivate() {
        String toSendAmountDescription, toSendAmount, toSendCode, toReceiveAmountDescription, toReceiveAmount, toReceiveCode;
        Monetary fixBaseSideAmount = model.getTakersBaseSideAmount();
        Monetary fixQuoteSideAmount = model.getTakersQuoteSideAmount();
        String formattedBaseAmount = AmountFormatter.formatBaseAmount(fixBaseSideAmount);
        String formattedQuoteAmount = AmountFormatter.formatQuoteAmount(fixQuoteSideAmount);
        Direction takersDirection = model.getMuSigOffer().getTakersDirection();
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
        closeAndNavigateToHandler.accept(NavigationTarget.MU_SIG_OPEN_TRADES);
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
