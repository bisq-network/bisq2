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

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.bonded_roles.market_price.NoMarketPriceAvailableException;
import bisq.common.market.Market;
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
import bisq.desktop.main.content.mu_sig.components.MuSigReviewDataDisplay;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.FixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
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

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MuSigTakeOfferReviewController implements Controller {
    private final MuSigTakeOfferReviewModel model;
    @Getter
    private final MuSigTakeOfferReviewView view;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final PriceInput priceInput;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final BannedUserService bannedUserService;
    private final MuSigReviewDataDisplay muSigReviewDataDisplay;
    private final MuSigService muSigService;
    private Pin errorMessagePin, peersErrorMessagePin;
    private UIScheduler timeoutScheduler;

    public MuSigTakeOfferReviewController(ServiceProvider serviceProvider,
                                          Consumer<Boolean> mainButtonsVisibleHandler,
                                          Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        muSigService = serviceProvider.getMuSigService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();

        priceInput = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());
        muSigReviewDataDisplay = new MuSigReviewDataDisplay();
        model = new MuSigTakeOfferReviewModel();
        view = new MuSigTakeOfferReviewView(model, this, muSigReviewDataDisplay.getRoot());
    }

    public void init(MuSigOffer muSigOffer) {
        model.setMuSigOffer(muSigOffer);
        Market market = muSigOffer.getMarket();
        priceInput.setMarket(market);

        String marketCodes = market.getMarketCodes();
        priceInput.setDescription(Res.get("bisqEasy.takeOffer.review.price.price", marketCodes));

        if (muSigOffer.getAmountSpec() instanceof FixedAmountSpec) {
            OfferAmountUtil.findBaseSideFixedAmount(marketPriceService, muSigOffer)
                    .ifPresent(model::setTakersBaseSideAmount);
            OfferAmountUtil.findQuoteSideFixedAmount(marketPriceService, muSigOffer)
                    .ifPresent(model::setTakersQuoteSideAmount);
        }

        Optional<PriceQuote> priceQuote = PriceUtil.findQuote(marketPriceService, muSigOffer);
        priceQuote.ifPresent(priceInput::setQuote);

        applyPriceQuote(priceQuote);
        applyPriceDetails(muSigOffer.getPriceSpec(), market);
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

    public void setTakersPaymentMethodSpec(PaymentMethodSpec<?> paymentMethodSpec) {
        if (paymentMethodSpec != null) {
            model.setTakersPaymentMethodSpec(paymentMethodSpec);
            model.setPaymentMethodDisplayString(paymentMethodSpec.getShortDisplayString());
            muSigReviewDataDisplay.setPaymentMethodDescription(Res.get("muSig.takeOffer.review.paymentMethod.description").toUpperCase());
            muSigReviewDataDisplay.setPaymentMethod(model.getPaymentMethodDisplayString());
        }
    }

    public void setTakersAccount(Account<?, ?> account) {
        if (account != null) {
            model.setTakersAccount(account);
            model.setPaymentMethodDetails(account.getAccountName());
        }
    }

    public void takeOffer(Runnable onCancelHandler) {
        MuSigOffer muSigOffer = model.getMuSigOffer();
        Monetary takersBaseSideAmount = model.getTakersBaseSideAmount();
        Monetary takersQuoteSideAmount = model.getTakersQuoteSideAmount();
        PaymentMethodSpec<?> paymentMethodSpec = model.getTakersPaymentMethodSpec();
        checkArgument(muSigOffer.getBaseSidePaymentMethodSpecs().size() == 1);
        mainButtonsVisibleHandler.accept(false);

        try {
            UserIdentity takerIdentity = userIdentityService.getSelectedUserIdentity();
            MuSigProtocol muSigProtocol = muSigService.takerCreatesProtocol(takerIdentity,
                    muSigOffer,
                    takersBaseSideAmount,
                    takersQuoteSideAmount,
                    paymentMethodSpec,
                    model.getTakersAccount());
            MuSigTrade trade = muSigProtocol.getTrade();
            model.setMuSigTrade(trade);
            muSigService.createMuSigOpenTradeChannel(trade, takerIdentity);

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

            errorMessagePin = trade.errorMessageObservable().addObserver(errorMessage -> {
                        if (errorMessage != null) {
                            UIThread.run(() -> {
                                if (trade.getTradeProtocolFailure() == null || trade.getTradeProtocolFailure().isUnexpected()) {
                                    String errorStackTrace = trade.getErrorStackTrace() != null ? StringUtils.truncate(trade.getErrorStackTrace(), 2000) : "";
                                    new Popup().error(Res.get("bisqEasy.openTrades.failed.errorPopup.message",
                                                    errorMessage,
                                                    errorStackTrace))
                                            .show();
                                } else {
                                    new Popup().headline(Res.get("bisqEasy.openTrades.failure.popup.headline"))
                                            .failure(Res.get("bisqEasy.openTrades.failure.popup.message.header"),
                                                    errorMessage,
                                                    Res.get("bisqEasy.openTrades.failure.popup.message.footer"))
                                            .show();
                                }
                            });
                        }
                    }
            );
            peersErrorMessagePin = trade.peersErrorMessageObservable().addObserver(peersErrorMessage -> {
                        if (peersErrorMessage != null) {
                            UIThread.run(() -> {
                                if (trade.getPeersTradeProtocolFailure() == null || trade.getPeersTradeProtocolFailure().isUnexpected()) {
                                    String errorStackTrace = trade.getPeersErrorStackTrace() != null ? StringUtils.truncate(trade.getPeersErrorStackTrace(), 2000) : "";
                                    new Popup().error(Res.get("bisqEasy.openTrades.failedAtPeer.errorPopup.message",
                                                    peersErrorMessage,
                                                    errorStackTrace))
                                            .show();
                                } else {
                                    new Popup().headline(Res.get("bisqEasy.openTrades.atPeer.failure.popup.headline"))
                                            .failure(Res.get("bisqEasy.openTrades.failure.popup.message.header"),
                                                    peersErrorMessage,
                                                    Res.get("bisqEasy.openTrades.failure.popup.message.footer"))
                                            .show();
                                }
                            });
                        }
                    }
            );

            // Start the protocol
            muSigService.takeOffer(trade);

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
                    String exceedsLimitInfo = bannedUserService.getExceedsLimitInfo(e.getUserProfileId()).orElseGet(() -> Res.get("data.na"));
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
                                    paymentMethodSpec,
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

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        String toSendAmountDescription, toSendAmount, toSendCode, toReceiveAmountDescription, toReceiveAmount, toReceiveCode;
        Monetary fixBaseSideAmount = model.getTakersBaseSideAmount();
        Monetary fixQuoteSideAmount = model.getTakersQuoteSideAmount();
        String formattedBaseAmount = AmountFormatter.formatBaseAmount(fixBaseSideAmount);
        String formattedQuoteAmount = AmountFormatter.formatQuoteAmount(fixQuoteSideAmount);
        Direction takersDirection = model.getMuSigOffer().getTakersDirection();
        if (takersDirection.isSell()) {
            toSendAmountDescription = Res.get("bisqEasy.tradeWizard.review.toSend");
            toReceiveAmountDescription = Res.get("bisqEasy.tradeWizard.review.toReceive");
            toSendAmount = formattedBaseAmount;
            toSendCode = fixBaseSideAmount.getCode();
            toReceiveAmount = formattedQuoteAmount;
            toReceiveCode = fixQuoteSideAmount.getCode();

            model.setFee(Res.get("bisqEasy.takeOffer.review.sellerPaysMinerFee"));
            model.setFeeDetails(Res.get("bisqEasy.takeOffer.review.noTradeFeesLong"));
        } else {
            toSendAmountDescription = Res.get("bisqEasy.tradeWizard.review.toPay");
            toReceiveAmountDescription = Res.get("bisqEasy.tradeWizard.review.toReceive");
            toSendAmount = formattedQuoteAmount;
            toSendCode = fixQuoteSideAmount.getCode();
            toReceiveAmount = formattedBaseAmount;
            toReceiveCode = fixBaseSideAmount.getCode();

            model.setFee(Res.get("bisqEasy.takeOffer.review.noTradeFees"));
            model.setFeeDetails(Res.get("bisqEasy.takeOffer.review.sellerPaysMinerFeeLong"));
        }

        String directionString = String.format("%s %s",
                Res.get(takersDirection.isSell() ? "offer.sell" : "offer.buy").toUpperCase(),
                model.getMuSigOffer().getMarket().getBaseCurrencyDisplayName());

        muSigReviewDataDisplay.setDirection(directionString);
        muSigReviewDataDisplay.setToSendAmountDescription(toSendAmountDescription.toUpperCase());
        muSigReviewDataDisplay.setToSendMaxOrFixedAmount(toSendAmount);
        muSigReviewDataDisplay.setToSendCode(toSendCode);
        muSigReviewDataDisplay.setToReceiveAmountDescription(toReceiveAmountDescription.toUpperCase());
        muSigReviewDataDisplay.setToReceiveMaxOrFixedAmount(toReceiveAmount);
        muSigReviewDataDisplay.setToReceiveCode(toReceiveCode);
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
        String marketPriceAsString = marketPriceQuote.map(PriceFormatter::formatWithCode).orElseGet(() -> Res.get("data.na"));
        Optional<Double> percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, priceSpec, market);
        double percent = percentFromMarketPrice.orElse(0d);
        if ((priceSpec instanceof FloatPriceSpec || priceSpec instanceof MarketPriceSpec) && percent == 0) {
            model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails", marketPriceAsString));
        } else {
            String aboveOrBelow = percent > 0 ? Res.get("offer.price.above") : Res.get("offer.price.below");
            String percentAsString = percentFromMarketPrice.map(Math::abs).map(PercentageFormatter::formatToPercentWithSymbol)
                    .orElseGet(() -> Res.get("data.na"));
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
