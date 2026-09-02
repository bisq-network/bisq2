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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer.review;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.network.identity.NetworkId;
import bisq.common.monetary.TradeAmount;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.mu_sig.offer.draft.components.MuSigPriceInput;
import bisq.desktop.main.content.mu_sig.offer.draft.components.MuSigReviewDataDisplay;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.use_case.take_offer.TakeOfferUseCase;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.settings.DontShowAgainKey;
import bisq.settings.DontShowAgainService;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.support.arbitration.mu_sig.NoMuSigArbitratorAvailableException;
import bisq.support.mediation.mu_sig.NoMuSigMediatorAvailableException;
import bisq.desktop.common.utils.TradeExceptionHandler;
import bisq.trade.TradeRestrictedException;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import bisq.user.banned.BannedUserService;
import bisq.user.banned.RateLimitExceededException;
import bisq.user.banned.UserProfileBannedException;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileIgnoredException;
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
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final MuSigPriceInput priceInput;
    private final TakeOfferUseCase takeOfferService;
    private Pin priceQuotePin;
    private Pin priceDeviationPin;
    private Pin fixTradeAmountPin;
    private final UserIdentityService userIdentityService;
    private final BannedUserService bannedUserService;
    private final MuSigReviewDataDisplay muSigReviewDataDisplay;
    private final MuSigService muSigService;
    private final DontShowAgainService dontShowAgainService;
    private Pin errorMessagePin, peersErrorMessagePin;
    private UIScheduler timeoutScheduler;
    private UIScheduler delayedSuccessScheduler;

    public MuSigTakeOfferReviewController(ServiceProvider serviceProvider,
                                          TakeOfferUseCase takeOfferService,
                                          Consumer<Boolean> mainButtonsVisibleHandler,
                                          Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        muSigService = serviceProvider.getMuSigService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        dontShowAgainService = serviceProvider.getDontShowAgainService();

        this.takeOfferService = takeOfferService;
        priceInput = new MuSigPriceInput(serviceProvider.getBondedRolesService().getMarketPriceService(), takeOfferService);
        muSigReviewDataDisplay = new MuSigReviewDataDisplay();

        model = new MuSigTakeOfferReviewModel();
        view = new MuSigTakeOfferReviewView(model, this, muSigReviewDataDisplay.getRoot());
    }

    public void init(MuSigOffer muSigOffer) {
        model.setMuSigOffer(muSigOffer);
        Market market = muSigOffer.getMarket();

        String marketCodes = market.getMarketCodes();
        priceInput.setDescription(Res.get("muSig.offer.taker.review.price.price", marketCodes));

        // The domain amount concern holds the taker's trade amount for fixed offers, collapsed
        // ranges and range selections alike; the observer registered in onActivate keeps it live.
        applyTakersAmountsFromDomain();

        Optional<PriceQuote> priceQuote = Optional.ofNullable(takeOfferService.getPriceService().getPriceQuote());
        priceQuote.ifPresent(priceInput::setQuote);

        applyPriceQuote(priceQuote);
        applyPriceDetails(muSigOffer.getPriceSpec(), market);


        double securityDeposit = OfferOptionUtil.findSymmetricSecurityDepositPercent(muSigOffer.getOfferOptions())
                .orElseThrow(() -> new IllegalArgumentException("CollateralOption must be present"));
        model.setSecurityDepositAsPercent(securityDeposit);
        model.setFormattedSecurityDepositAsPercent(PercentageFormatter.formatToPercentWithSymbol(securityDeposit, 0));

        applySecurityDepositAsBtc();
    }

    public void setTakersPaymentMethodSpec(PaymentMethodSpec<?> paymentMethodSpec) {
        if (paymentMethodSpec != null) {
            model.setTakersPaymentMethodSpec(paymentMethodSpec);
            model.setPaymentMethodDisplayString(paymentMethodSpec.getShortDisplayString());
            muSigReviewDataDisplay.setPaymentMethodDescription(Res.get("muSig.offer.taker.review.paymentMethod.description").toUpperCase());
            muSigReviewDataDisplay.setPaymentMethod(model.getPaymentMethodDisplayString());
        }
    }

    public void setTakersAccount(Account<?, ?> account) {
        if (account != null) {
            model.setTakersAccount(account);
            model.setPaymentMethodDetails(account.getAccountName());
        }
    }

    public void takeOffer() {
        if (!confirmationAllowed()) {
            return;
        }
        MuSigOffer muSigOffer = model.getMuSigOffer();
        UserIdentity takerIdentity = userIdentityService.getSelectedUserIdentity();
        // Already-taken is a soft confirm: the taker may retake (the trade id includes the take
        // date, so a retake gets a fresh id). Shown once unless muted (take-offer.md, Review).
        NetworkId takerNetworkId = takerIdentity.getUserProfile().getNetworkId();
        if (muSigService.wasOfferAlreadyTaken(muSigOffer, takerNetworkId)
                && dontShowAgainService.showAgain(DontShowAgainKey.OFFER_ALREADY_TAKEN_WARN)) {
            new Popup().information(Res.get("muSig.offer.taker.offerAlreadyTaken.info"))
                    .actionButtonText(Res.get("confirmation.yes"))
                    .onAction(() -> doTakeOffer(false))
                    .closeButtonText(Res.get("confirmation.no"))
                    .dontShowAgainId(DontShowAgainKey.OFFER_ALREADY_TAKEN_WARN)
                    .owner(view.getRoot())
                    .show();
            return;
        }
        doTakeOffer(false);
    }

    // Re-checked at every entry, including after the async already-taken popup: a market price
    // update between opening the popup and confirming can remove the price or invalidate the
    // amount (take-offer.md, "Amount limits", background changes).
    private boolean confirmationAllowed() {
        // A submission is already in flight or has succeeded. Hiding the buttons does not
        // disable the parent's Enter-key handler, so repeated input could otherwise create a
        // second trade for the same offer (the trade id contains the take date, so a resend is
        // a new trade, not a duplicate message). The failure path resets the status, which
        // re-opens the gate for a legitimate retry.
        if (model.getTakeOfferStatus().get() != MuSigTakeOfferReviewModel.TakeOfferStatus.NOT_STARTED) {
            return false;
        }
        // Runtime invalidation gate: without a current market price the take cannot proceed
        // (take-offer.md, Price). The block lifts as soon as a price arrives again.
        if (takeOfferService.getPriceService().getMarketPriceQuote() == null) {
            new Popup().warning(Res.get("muSig.takeOffer.validation.noMarketPrice"))
                    .owner(view.getRoot())
                    .show();
            return false;
        }
        // Background invalidation gate: a market price update (or a payment method change on a
        // fixed offer) can push the amount outside the effective limits; the amount is never
        // clamped, confirmation is blocked instead and the block lifts on recovery.
        if (!takeOfferService.getAmountService().isAmountValid()) {
            new Popup().warning(Res.get("muSig.takeOffer.validation.amountOutsideLimits"))
                    .owner(view.getRoot())
                    .show();
            return false;
        }
        // The take can only proceed with the use case's validated account and payment method
        // spec, which come from the same selection entry and are therefore mutually consistent.
        // A deep navigation to the review step could otherwise bypass the payment step's own
        // validation and reach here with no (or a stale) selection.
        if (takeOfferService.getSelectedAccount().isEmpty()
                || takeOfferService.getSelectedPaymentMethodSpec().isEmpty()) {
            new Popup().warning(Res.get("muSig.takeOffer.validation.noPaymentAccount"))
                    .owner(view.getRoot())
                    .show();
            return false;
        }
        return true;
    }

    private void doTakeOffer(boolean proceedWithoutMediator) {
        if (!confirmationAllowed()) {
            return;
        }
        MuSigOffer muSigOffer = model.getMuSigOffer();
        // Source the account and spec from the use case (validated, mutually consistent) rather
        // than the review model, whose display fields can lag a payment method switch.
        PaymentMethodSpec<?> paymentMethodSpec = takeOfferService.getSelectedPaymentMethodSpec().orElseThrow();
        Account<?, ?> takersAccount = takeOfferService.getSelectedAccount().orElseThrow();
        // The amounts and the market price they were validated against are captured as one
        // atomic snapshot: a market-price update on another thread mutates them in several steps,
        // so reading them separately could hand off a torn pair (take-offer.md, "Handoff").
        TakeOfferUseCase.Handoff handoff = takeOfferService.getHandoff().orElse(null);
        if (handoff == null) {
            new Popup().warning(Res.get("muSig.takeOffer.validation.amountOutsideLimits"))
                    .owner(view.getRoot())
                    .show();
            return;
        }
        Monetary takersBaseSideAmount = handoff.baseSideAmount();
        Monetary takersQuoteSideAmount = handoff.quoteSideAmount();
        long marketPrice = handoff.marketPrice();

        try {
            UserIdentity takerIdentity = userIdentityService.getSelectedUserIdentity();
            MuSigProtocol muSigProtocol = muSigService.takerCreatesProtocol(takerIdentity,
                    muSigOffer,
                    takersBaseSideAmount,
                    takersQuoteSideAmount,
                    paymentMethodSpec,
                    takersAccount,
                    marketPrice,
                    proceedWithoutMediator);
            MuSigTrade trade = muSigProtocol.getTrade();
            model.setMuSigTrade(trade);
            muSigService.createMuSigOpenTradeChannel(trade, takerIdentity);

            if (timeoutScheduler != null) {
                timeoutScheduler.stop();
            }
            timeoutScheduler = UIScheduler.run(() -> {
                        if (model.getMuSigTrade() != trade) {
                            return;
                        }
                        closeAndNavigateToHandler.accept(NavigationTarget.MU_SIG);
                        new Popup().warning(Res.get("muSig.offer.taker.timeout.warning", 150)).show();
                    })
                    .after(150, TimeUnit.SECONDS);
            // We have 120 seconds socket timeout, so we should never
            // get triggered here, as the message will be sent as mailbox message

            // A previous attempt's callbacks must not act on this attempt (retry case): the
            // unbind below stops future notifications, but a runnable the old observer already
            // queued on the JavaFX thread still runs afterwards - every deferred callback
            // therefore re-checks that its trade is still the current attempt before touching
            // the shared schedulers, status or popups.
            if (errorMessagePin != null) {
                errorMessagePin.unbind();
            }
            if (peersErrorMessagePin != null) {
                peersErrorMessagePin.unbind();
            }
            errorMessagePin = trade.errorMessageObservable().addObserver(errorMessage -> {
                        if (errorMessage != null) {
                            UIThread.run(() -> {
                                if (model.getMuSigTrade() != trade) {
                                    return;
                                }
                                resetTakeOfferStatusOnFailure();
                                if (trade.getTradeProtocolFailure() == null || trade.getTradeProtocolFailure().isUnexpected()) {
                                    String errorStackTrace = trade.getErrorStackTrace() != null ? StringUtils.truncate(trade.getErrorStackTrace(), 2000) : "";
                                    new Popup().error(Res.get("muSig.trade.pending.failed.errorPopup.message",
                                                    errorMessage,
                                                    errorStackTrace))
                                            .show();
                                } else {
                                    new Popup().headline(Res.get("muSig.trade.pending.failure.popup.headline"))
                                            .failure(Res.get("muSig.trade.pending.failure.popup.message.header"),
                                                    errorMessage,
                                                    Res.get("muSig.trade.pending.failure.popup.message.footer"))
                                            .show();
                                }
                            });
                        }
                    }
            );
            peersErrorMessagePin = trade.peersErrorMessageObservable().addObserver(peersErrorMessage -> {
                        if (peersErrorMessage != null) {
                            UIThread.run(() -> {
                                if (model.getMuSigTrade() != trade) {
                                    return;
                                }
                                resetTakeOfferStatusOnFailure();
                                if (trade.getPeersTradeProtocolFailure() == null || trade.getPeersTradeProtocolFailure().isUnexpected()) {
                                    String errorStackTrace = trade.getPeersErrorStackTrace() != null ? StringUtils.truncate(trade.getPeersErrorStackTrace(), 2000) : "";
                                    new Popup().error(Res.get("muSig.trade.pending.failedAtPeer.errorPopup.message",
                                                    peersErrorMessage,
                                                    errorStackTrace))
                                            .show();
                                } else {
                                    new Popup().headline(Res.get("muSig.trade.pending.failure.popup.headline.atPeer"))
                                            .failure(Res.get("muSig.trade.pending.failure.popup.message.header"),
                                                    peersErrorMessage,
                                                    Res.get("muSig.trade.pending.failure.popup.message.footer"))
                                            .show();
                                }
                            });
                        }
                    }
            );

            // Start the protocol
            muSigService.takeOffer(trade);

            // Hide the navigation buttons only after the synchronous validation passed, so an abort
            // above leaves the overlay with its Close button intact.
            mainButtonsVisibleHandler.accept(false);

            // todo We send the protocol message and log message inside the protocol handler and don't have an easy way
            //  to get notified about the delivery state.
            model.getTakeOfferStatus().set(MuSigTakeOfferReviewModel.TakeOfferStatus.SENT);
            // todo simulate a small delay until we have a solution for the above issue
            if (delayedSuccessScheduler != null) {
                delayedSuccessScheduler.stop();
            }
            delayedSuccessScheduler = UIScheduler.run(() -> {
                if (model.getMuSigTrade() != trade) {
                    return;
                }
                if (trade.getErrorMessage() != null || trade.getPeersErrorMessage() != null) {
                    // The maker rejected the take offer; the error observer already informed the user.
                    return;
                }
                // The attempt succeeded; the timeout must not navigate away from the success
                // screen later with a warning about this same attempt.
                if (timeoutScheduler != null) {
                    timeoutScheduler.stop();
                }
                model.getTakeOfferStatus().set(MuSigTakeOfferReviewModel.TakeOfferStatus.SUCCESS);
            }).after(200);
        } catch (TradeRestrictedException e) {
            // The timeout scheduler and error observers were already set up above; release them so
            // the aborted attempt cannot fire the timeout navigation later or stack observers on retry.
            if (timeoutScheduler != null) {
                timeoutScheduler.stop();
            }
            if (errorMessagePin != null) {
                errorMessagePin.unbind();
                errorMessagePin = null;
            }
            if (peersErrorMessagePin != null) {
                peersErrorMessagePin.unbind();
                peersErrorMessagePin = null;
            }
            UIThread.run(() -> new Popup().warning(TradeExceptionHandler.localizedMessage(e)).show());
        } catch (UserProfileBannedException e) {
            UIThread.run(() -> {
                if (muSigOffer.getMakersUserProfileId().equals(e.getUserProfileId())) {
                    new Popup().warning(Res.get("muSig.offer.taker.banned.maker.warning")).show();
                } else {
                    // We do not inform banned users about being banned
                    log.debug("Takers user profile was banned");
                }
            });
        } catch (UserProfileIgnoredException e) {
            UIThread.run(() -> new Popup().warning(Res.get("muSig.offer.taker.ignored.maker.warning")).show());
        } catch (RateLimitExceededException e) {
            UIThread.run(() -> {
                if (muSigOffer.getMakersUserProfileId().equals(e.getUserProfileId())) {
                    new Popup().warning(Res.get("muSig.offer.taker.rateLimitsExceeded.maker.warning")).show();
                } else {
                    String exceedsLimitInfo = bannedUserService.getExceedsLimitInfo(e.getUserProfileId()).orElseGet(() -> Res.get("data.na"));
                    new Popup().warning(Res.get("muSig.offer.taker.rateLimitsExceeded.taker.warning", exceedsLimitInfo)).show();
                }
            });
        } catch (NoMuSigMediatorAvailableException e) {
            // Proceeding unmediated needs explicit consent; the retry re-enters through the
            // confirmation gates and takes a fresh handoff snapshot.
            UIThread.run(() -> new Popup().warning(Res.get("muSig.offer.taker.noMediatorAvailable.warning"))
                    .closeButtonText(Res.get("action.cancel"))
                    .actionButtonText(Res.get("muSig.offer.taker.noMediatorAvailable.proceed"))
                    .onAction(() -> doTakeOffer(true))
                    .show());
        } catch (NoMuSigArbitratorAvailableException e) {
            UIThread.run(() -> new Popup().warning(Res.get("muSig.offer.taker.noArbitratorAvailable.warning")).show());
        } catch (RuntimeException e) {
            // Any other synchronous failure (e.g. channel creation failing because the maker
            // profile is gone) must release this attempt like the named business exceptions do,
            // so the aborted attempt cannot fire the timeout later or stack observers on retry.
            // The trade may already be persisted at this point; that residue is protocol-level
            // and surfaces like any other failed take attempt.
            if (timeoutScheduler != null) {
                timeoutScheduler.stop();
            }
            if (errorMessagePin != null) {
                errorMessagePin.unbind();
                errorMessagePin = null;
            }
            if (peersErrorMessagePin != null) {
                peersErrorMessagePin.unbind();
                peersErrorMessagePin = null;
            }
            throw e;
        }
    }

    public void reset() {
        model.reset();
    }

    private void resetTakeOfferStatusOnFailure() {
        if (timeoutScheduler != null) {
            timeoutScheduler.stop();
        }
        if (delayedSuccessScheduler != null) {
            delayedSuccessScheduler.stop();
        }
        mainButtonsVisibleHandler.accept(true);
        model.getTakeOfferStatus().set(MuSigTakeOfferReviewModel.TakeOfferStatus.NOT_STARTED);
    }

    @Override
    public void onActivate() {
        priceQuotePin = takeOfferService.getPriceService().priceQuoteObservable().addObserver(quote ->
                UIThread.run(this::refreshPriceDisplay));
        // Fixed-price offers keep their quote when the market price moves, so the equal-value
        // suppression of the quote observable would starve the detail refresh; the deviation
        // changes on every relevant market update.
        priceDeviationPin = takeOfferService.getPriceService().priceDeviationObservable().addObserver(deviation ->
                UIThread.run(this::refreshPriceDisplay));
        // Covers range selections made in the amount step, clamps after a payment method change
        // and background passive-side refreshes; fires at registration for the initial state.
        fixTradeAmountPin = takeOfferService.getAmountService().fixTradeAmountObservable().addObserver(tradeAmount ->
                UIThread.run(this::applyTakersAmountsFromDomain));
        applyAmountsAndDisplay();
    }

    private void applyAmountsAndDisplay() {
        String toSendAmountDescription, toSendAmount, toSendCode, toReceiveAmountDescription, toReceiveAmount, toReceiveCode;
        Monetary fixBaseSideAmount = model.getTakersBaseSideAmount();
        Monetary fixQuoteSideAmount = model.getTakersQuoteSideAmount();
        String formattedBaseAmount = AmountFormatter.formatBaseAmount(fixBaseSideAmount);
        String formattedQuoteAmount = AmountFormatter.formatQuoteAmount(fixQuoteSideAmount);
        Direction takersDirection = model.getMuSigOffer().getTakersDisplayDirection();
        if (takersDirection.isSell()) {
            toSendAmountDescription = Res.get("muSig.offer.wizard.review.toSend");
            toReceiveAmountDescription = Res.get("muSig.offer.wizard.review.toReceive");
            toSendAmount = formattedBaseAmount;
            toSendCode = fixBaseSideAmount.getCode();
            toReceiveAmount = formattedQuoteAmount;
            toReceiveCode = fixQuoteSideAmount.getCode();
        } else {
            toSendAmountDescription = Res.get("muSig.offer.wizard.review.toPay");
            toReceiveAmountDescription = Res.get("muSig.offer.wizard.review.toReceive");
            toSendAmount = formattedQuoteAmount;
            toSendCode = fixQuoteSideAmount.getCode();
            toReceiveAmount = formattedBaseAmount;
            toReceiveCode = fixBaseSideAmount.getCode();
        }

        // The review deliberately omits a numeric trade fee until the UI and protocol share an
        // authoritative fee policy. Showing the protocol placeholder or a separate UI estimate
        // would present a value that the trade does not consistently enforce.
        model.setFee(Res.get("data.na"));
        // The mining fee is charged to the Bitcoin seller. The display direction is
        // altcoin-denominated in Altcoin-Bitcoin markets, so the fee note keys on the
        // Bitcoin-side taker direction (the mirror of the Bitcoin-side offer direction).
        model.setFeeDetails(model.getMuSigOffer().getTakersDirection().isSell()
                ? Res.get("muSig.offer.taker.review.takerPaysMinerFee")
                : Res.get("muSig.offer.taker.review.sellerPaysMinerFeeLong"));

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
        muSigReviewDataDisplay.setPriceDescription(Res.get("muSig.offer.taker.review.price.price").toUpperCase());
        muSigReviewDataDisplay.setPrice(model.getPrice());
        muSigReviewDataDisplay.setPriceCode(model.getPriceCode());
        muSigReviewDataDisplay.setIsCryptoMarket(model.getMuSigOffer().getMarket().isCrypto());
    }

    @Override
    public void onDeactivate() {
        if (priceQuotePin != null) {
            priceQuotePin.unbind();
            priceQuotePin = null;
        }
        if (priceDeviationPin != null) {
            priceDeviationPin.unbind();
            priceDeviationPin = null;
        }
        if (fixTradeAmountPin != null) {
            fixTradeAmountPin.unbind();
            fixTradeAmountPin = null;
        }
        if (errorMessagePin != null) {
            errorMessagePin.unbind();
        }
        if (peersErrorMessagePin != null) {
            peersErrorMessagePin.unbind();
        }
        if (timeoutScheduler != null) {
            timeoutScheduler.stop();
        }
        if (delayedSuccessScheduler != null) {
            delayedSuccessScheduler.stop();
        }
    }

    void onShowOpenTrades() {
        closeAndNavigateToHandler.accept(NavigationTarget.MU_SIG_OPEN_TRADES);
    }

    private void refreshPriceDisplay() {
        PriceQuote quote = takeOfferService.getPriceService().getPriceQuote();
        if (quote == null || model.getMuSigOffer() == null) {
            return;
        }
        priceInput.setQuote(quote);
        applyPriceQuote(Optional.of(quote));
        applyPriceDetails(model.getMuSigOffer().getPriceSpec(), model.getMuSigOffer().getMarket());
        applySecurityDepositAsBtc();
        applyAmountsAndDisplay();
    }

    private void applyTakersAmountsFromDomain() {
        // The observer queues this through UIThread.run; unbinding does not cancel an already
        // queued call, which can therefore run after reset() cleared the model.
        if (model.getMuSigOffer() == null) {
            return;
        }
        TradeAmount fixTradeAmount = takeOfferService.getAmountService().getFixTradeAmount();
        if (fixTradeAmount == null) {
            return;
        }
        model.setTakersBaseSideAmount(fixTradeAmount.getBaseSideAmount());
        model.setTakersQuoteSideAmount(fixTradeAmount.getQuoteSideAmount());
        applySecurityDepositAsBtc();
        applyAmountsAndDisplay();
    }

    private void applyPriceDetails(PriceSpec priceSpec, Market market) {
        // Market price and deviation come from the take offer use case so the displayed details
        // always match the snapshot behind the resolved quote and the warning state.
        Optional<PriceQuote> marketPriceQuote = Optional.ofNullable(takeOfferService.getPriceService().getMarketPriceQuote());
        marketPriceQuote.ifPresent(quote -> model.setMarketPrice(quote.getValue()));
        String marketPriceAsString = marketPriceQuote.map(PriceFormatter::formatWithCode).orElseGet(() -> Res.get("data.na"));
        Optional<Double> percentFromMarketPrice = Optional.ofNullable(takeOfferService.getPriceService().getPriceDeviation());
        if (percentFromMarketPrice.isEmpty()) {
            // The market price vanished mid-flow (deviation and market quote are cleared
            // together): no relation to the market price can be claimed, in particular not
            // "same as market price". Confirmation is blocked separately.
            model.setPriceDetails(Res.get("data.na"));
            return;
        }
        double percent = percentFromMarketPrice.get();
        if ((priceSpec instanceof FloatPriceSpec || priceSpec instanceof MarketPriceSpec) && percent == 0) {
            model.setPriceDetails(Res.get("muSig.offer.wizard.review.priceDetails", marketPriceAsString));
        } else {
            String aboveOrBelow = percent > 0 ? Res.get("offer.price.above") : Res.get("offer.price.below");
            String percentAsString = percentFromMarketPrice.map(Math::abs).map(PercentageFormatter::formatToPercentWithSymbol)
                    .orElseGet(() -> Res.get("data.na"));
            if (priceSpec instanceof FloatPriceSpec) {
                model.setPriceDetails(Res.get("muSig.offer.wizard.review.priceDetails.float",
                        percentAsString, aboveOrBelow, marketPriceAsString));
            } else {
                if (percent == 0) {
                    model.setPriceDetails(Res.get("muSig.offer.wizard.review.priceDetails.fix.atMarket",
                            marketPriceAsString));
                } else {
                    model.setPriceDetails(Res.get("muSig.offer.wizard.review.priceDetails.fix",
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
        model.setPriceWithCode(Res.get("muSig.offer.wizard.review.price", formattedPrice, codes));
        model.setPrice(formattedPrice);
        model.setPriceCode(codes);
    }

    private void applySecurityDepositAsBtc() {
        double securityDeposit = model.getSecurityDepositAsPercent();
        Market market = model.getMuSigOffer().getMarket();
        Monetary takersBaseSideAmount = model.getTakersBaseSideAmount();
        Monetary takersQuoteSideAmount = model.getTakersQuoteSideAmount();
        if (takersBaseSideAmount != null && takersQuoteSideAmount != null) {
            model.setSecurityDepositAsBtc(calculateSecurityDeposit(market,
                    takersBaseSideAmount,
                    takersQuoteSideAmount,
                    securityDeposit));
        }
    }

    private static String calculateSecurityDeposit(Market market,
                                                   Monetary takersBaseSideAmount,
                                                   Monetary takersQuoteSideAmount,
                                                   double securityDeposit) {
        Monetary securityDepositAsBtc = OfferAmountUtil.calculateSecurityDepositAsBTC(market,
                takersBaseSideAmount,
                takersQuoteSideAmount,
                securityDeposit);
        return OfferAmountFormatter.formatDepositAmountAsBTC(securityDepositAsBtc);
    }
}
