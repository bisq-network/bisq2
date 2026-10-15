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

package bisq.desktop.main.content.mu_sig.offer.draft.create_offer.review;

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpecFormatter;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.mu_sig.offer.draft.components.MuSigReviewDataDisplay;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.use_case.create_offer.CreateOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.DraftSnapshot;
import bisq.offer.options.AccountOption;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.user.banned.RateLimitExceededException;
import bisq.user.banned.UserProfileBannedException;
import com.google.common.base.Joiner;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class MuSigCreateOfferReviewController implements Controller {
    private final MuSigCreateOfferReviewModel model;
    @Getter
    private final MuSigCreateOfferReviewView view;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final MuSigReviewDataDisplay muSigReviewDataDisplay;
    private final MuSigService muSigService;
    private final CreateOfferUseCase createOfferUseCase;

    public MuSigCreateOfferReviewController(ServiceProvider serviceProvider,
                                            CreateOfferUseCase createOfferUseCase,
                                            Consumer<Boolean> mainButtonsVisibleHandler,
                                            Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.createOfferUseCase = createOfferUseCase;

        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;
        muSigService = serviceProvider.getMuSigService();

        muSigReviewDataDisplay = new MuSigReviewDataDisplay();

        model = new MuSigCreateOfferReviewModel();
        view = new MuSigCreateOfferReviewView(model, this, muSigReviewDataDisplay.getRoot());
    }

    public void initialize() {
        // One synchronized capture: a concurrent market-price update cannot produce mixed
        // review values or an offer inconsistent with what is displayed.
        DraftSnapshot snapshot = createOfferUseCase.captureDraftSnapshot();
        Market market = snapshot.market();
        Direction displayDirection = snapshot.displayDirection();
        AmountSpec amountSpec = snapshot.amountSpec();
        PriceSpec priceSpec = snapshot.priceSpec();

        Map<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethod = snapshot.accountByPaymentMethod();
        List<PaymentMethod<?>> paymentMethods = accountByPaymentMethod.keySet().stream()
                .sorted(Comparator.comparing(PaymentMethod::getPaymentRailName))
                .toList();
        model.setPaymentMethods(paymentMethods);
        model.setPaymentMethodDescription(
                paymentMethods.size() == 1
                        ? Res.get("muSig.offer.create.review.paymentMethod.description")
                        : Res.get("muSig.offer.create.review.paymentMethods.description")
        );
        verifyPaymentMethods(paymentMethods);

        List<String> accountNames = accountByPaymentMethod.values().stream()
                .sorted(Comparator.comparing(o -> o.getPaymentMethod().getPaymentRailName()))
                .map(Account::getAccountName)
                .toList();
        model.setPaymentMethodDetails(Joiner.on(", ").join(accountNames));


        model.setPaymentMethodsDisplayString(PaymentMethodSpecFormatter.fromPaymentMethods(paymentMethods));

        applyData(snapshot);

        String offerId = StringUtils.createUid();
        List<OfferOption> offerOptions = accountByPaymentMethod.values().stream()
                .map(account -> {
                    AccountPayload<?> accountPayload = account.getAccountPayload();
                    String saltedAccountId = OfferOptionUtil.createdSaltedAccountId(account.getId(), offerId);
                    Optional<String> countryCode = AccountUtils.getCountryCode(accountPayload);
                    List<String> acceptedCountryCodes = AccountUtils.getAcceptedCountryCodes(accountPayload);
                    Optional<String> bankId = AccountUtils.getBankId(accountPayload);
                    List<String> acceptedBanks = AccountUtils.getAcceptedBanks(accountPayload);
                    byte[] saltedAccountPayloadHash = OfferOptionUtil.createSaltedAccountPayloadHash(accountPayload, offerId);
                    return new AccountOption(
                            account.getPaymentMethod(),
                            saltedAccountId,
                            countryCode,
                            acceptedCountryCodes,
                            bankId,
                            acceptedBanks,
                            saltedAccountPayloadHash);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // We use static values for both traders of 25%
        offerOptions.add(new CollateralOption(model.getSecurityDepositAsPercent(), model.getSecurityDepositAsPercent()));

        Direction offerDirection = Direction.displayDirectionToOfferDirection(displayDirection, market);
        MuSigOffer offer = muSigService.createAndGetMuSigOffer(offerId,
                offerDirection,
                market,
                amountSpec,
                priceSpec,
                paymentMethods,
                offerOptions);
        model.setOffer(offer);

        if (displayDirection.isSell()) {
            model.setFee(Res.get("muSig.offer.create.review.sellerPaysMinerFee"));
            model.setFeeDetails(Res.get("muSig.offer.create.review.noTradeFeesLong"));
        } else {
            model.setFee(Res.get("muSig.offer.create.review.noTradeFees"));
            model.setFeeDetails(Res.get("muSig.offer.create.review.sellerPaysMinerFeeLong"));
        }
    }

    public void publishOffer() {
        MuSigOffer muSigOffer = model.getOffer();
        try {
            muSigService.publishOffer(muSigOffer).whenComplete((result, throwable) -> {
                if (throwable == null) {
                    UIThread.run(() -> {
                        model.getShowCreateOfferSuccess().set(true);
                        mainButtonsVisibleHandler.accept(false);
                        muSigService.getSelectedMuSigOffer().set(muSigOffer);
                    });
                    result.forEach(future -> {
                        future.whenComplete((res, t) -> {
                            if (t == null) {
                                log.info("Offer published. result={}", res);
                            } else {
                                log.error("Offer publishing failed", t);
                            }
                        });
                    });
                } else {
                    log.error("Offer publishing failed", throwable);
                }
            });
        } catch (UserProfileBannedException e) {
            // We do not inform banned users about being banned
        } catch (RateLimitExceededException e) {
            UIThread.run(() -> new Popup().warning(Res.get("muSig.offer.create.rateLimitsExceeded.publish.warning")).show());
        }
    }

    // direction is from user perspective not offer direction
    private void applyData(DraftSnapshot snapshot) {
        Direction displayDirection = snapshot.displayDirection();
        AmountSpec amountSpec = snapshot.amountSpec();
        PriceSpec priceSpec = snapshot.priceSpec();
        Market market = snapshot.market();

        model.setCrypto(market.isCrypto());

        model.setPriceSpec(priceSpec);

        Optional<PriceQuote> priceQuote = snapshot.resolvedPriceQuote();
        String formattedPrice = priceQuote
                .map(PriceFormatter::format)
                .orElse("");
        String codes = priceQuote.map(e -> e.getMarket().getMarketCodes()).orElse("");
        model.setPriceWithCode(Res.get("muSig.offer.wizard.review.price", formattedPrice, codes));
        model.setPrice(formattedPrice);
        model.setPriceCode(codes);

        applyPriceDetails(snapshot);

        // DEFAULT_BUYER_SECURITY_DEPOSIT and DEFAULT_SELLER_SECURITY_DEPOSIT are the same
        double securityDeposit = MuSigOffer.DEFAULT_BUYER_SECURITY_DEPOSIT;
        model.setSecurityDepositAsPercent(securityDeposit);
        String securityDepositAsPercent = PercentageFormatter.formatToPercentWithSymbol(securityDeposit, 0);
        model.setFormattedSecurityDepositAsPercent(securityDepositAsPercent);

        model.setRangeAmount(amountSpec instanceof RangeAmountSpec);
        String currentToSendMinAmount = null, currentToReceiveMinAmount = null,
                currentToReceiveMaxOrFixedAmount, currentToSendMaxOrFixedAmount,
                toSendAmountDescription, toSendCode, toReceiveAmountDescription, toReceiveCode;
        PriceQuote resolvedQuote = priceQuote.orElseThrow();
        if (model.isRangeAmount()) {
            Monetary minBaseSideAmount = OfferAmountUtil.findBaseSideMinAmount(resolvedQuote, amountSpec, market).orElseThrow();
            model.setMinBaseSideAmount(minBaseSideAmount);
            Monetary maxBaseSideAmount = OfferAmountUtil.findBaseSideMaxAmount(resolvedQuote, amountSpec, market).orElseThrow();
            model.setMaxBaseSideAmount(maxBaseSideAmount);

            Monetary minQuoteSideAmount = OfferAmountUtil.findQuoteSideMinAmount(resolvedQuote, amountSpec, market).orElseThrow();
            model.setMinQuoteSideAmount(minQuoteSideAmount);
            Monetary maxQuoteSideAmount = OfferAmountUtil.findQuoteSideMaxAmount(resolvedQuote, amountSpec, market).orElseThrow();
            model.setMaxQuoteSideAmount(maxQuoteSideAmount);

            String formattedMinQuoteAmount = AmountFormatter.formatQuoteAmount(minQuoteSideAmount);
            String formattedMinBaseAmount = AmountFormatter.formatBaseAmount(minBaseSideAmount);
            String formattedMaxQuoteAmount = AmountFormatter.formatQuoteAmount(maxQuoteSideAmount);
            String formattedMaxBaseAmount = AmountFormatter.formatBaseAmount(maxBaseSideAmount);
            if (displayDirection.isSell()) {
                currentToSendMinAmount = formattedMinBaseAmount;
                currentToSendMaxOrFixedAmount = formattedMaxBaseAmount;
                currentToReceiveMinAmount = formattedMinQuoteAmount;
                currentToReceiveMaxOrFixedAmount = formattedMaxQuoteAmount;

                toSendCode = maxBaseSideAmount.getCode();
                toReceiveCode = maxQuoteSideAmount.getCode();
            } else {
                currentToSendMinAmount = formattedMinQuoteAmount;
                currentToSendMaxOrFixedAmount = formattedMaxQuoteAmount;
                currentToReceiveMinAmount = formattedMinBaseAmount;
                currentToReceiveMaxOrFixedAmount = formattedMaxBaseAmount;

                toSendCode = maxQuoteSideAmount.getCode();
                toReceiveCode = maxBaseSideAmount.getCode();
            }

            model.setSecurityDepositAsBtc(calculateSecurityDeposit(market, minBaseSideAmount, minQuoteSideAmount) + " - " +
                    calculateSecurityDeposit(market, maxBaseSideAmount, maxQuoteSideAmount));
        } else {
            Monetary fixBaseSideAmount = OfferAmountUtil.findBaseSideFixedAmount(resolvedQuote, amountSpec, market).orElseThrow();
            model.setFixBaseSideAmount(fixBaseSideAmount);
            String formattedBaseAmount = AmountFormatter.formatBaseAmount(fixBaseSideAmount);

            Monetary fixQuoteSideAmount = OfferAmountUtil.findQuoteSideFixedAmount(resolvedQuote, amountSpec, market).orElseThrow();
            model.setFixQuoteSideAmount(fixQuoteSideAmount);
            String formattedQuoteAmount = AmountFormatter.formatQuoteAmount(fixQuoteSideAmount);

            if (displayDirection.isSell()) {
                currentToSendMaxOrFixedAmount = formattedBaseAmount;
                toSendCode = fixBaseSideAmount.getCode();
                currentToReceiveMaxOrFixedAmount = formattedQuoteAmount;
                toReceiveCode = fixQuoteSideAmount.getCode();
            } else {
                currentToSendMaxOrFixedAmount = formattedQuoteAmount;
                toSendCode = fixQuoteSideAmount.getCode();
                currentToReceiveMaxOrFixedAmount = formattedBaseAmount;
                toReceiveCode = fixBaseSideAmount.getCode();
            }

            model.setSecurityDepositAsBtc(calculateSecurityDeposit(market, fixBaseSideAmount, fixQuoteSideAmount));
        }

        model.setHeadline(Res.get("muSig.offer.create.review.headline.maker"));
        model.setDetailsHeadline(Res.get("muSig.offer.create.review.detailsHeadline.maker").toUpperCase());

        model.setPriceDescription(Res.get("muSig.offer.create.review.priceDescription.maker"));
        if (displayDirection.isSell()) {
            toSendAmountDescription = Res.get("muSig.offer.wizard.review.toSend");
        } else {
            toSendAmountDescription = Res.get("muSig.offer.wizard.review.toPay");
        }
        toReceiveAmountDescription = Res.get("muSig.offer.wizard.review.toReceive");

        String directionString = String.format("%s %s",
                Res.get(displayDirection.isSell() ? "offer.sell" : "offer.buy").toUpperCase(),
                market.getBaseCurrencyDisplayName());

        applyHeaderPaymentMethod();

        muSigReviewDataDisplay.setToSendMinAmount(currentToSendMinAmount);
        muSigReviewDataDisplay.setToReceiveMinAmount(currentToReceiveMinAmount);
        muSigReviewDataDisplay.setRangeAmount(model.isRangeAmount());
        muSigReviewDataDisplay.setDirection(directionString);
        muSigReviewDataDisplay.setToSendAmountDescription(toSendAmountDescription.toUpperCase());
        muSigReviewDataDisplay.setToSendMaxOrFixedAmount(currentToSendMaxOrFixedAmount);
        muSigReviewDataDisplay.setToSendCode(toSendCode);
        muSigReviewDataDisplay.setToReceiveAmountDescription(toReceiveAmountDescription.toUpperCase());
        muSigReviewDataDisplay.setToReceiveMaxOrFixedAmount(currentToReceiveMaxOrFixedAmount);
        muSigReviewDataDisplay.setToReceiveCode(toReceiveCode);
        muSigReviewDataDisplay.setPaymentMethodDescription(model.getPaymentMethodDescription().toUpperCase());
        muSigReviewDataDisplay.setPriceDescription(model.getPriceDescription().toUpperCase());
        muSigReviewDataDisplay.setPrice(model.getPrice());
        muSigReviewDataDisplay.setPriceCode(model.getPriceCode());
        muSigReviewDataDisplay.setIsCryptoMarket(market.isCrypto());
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        model.getShowCreateOfferSuccess().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    void onShowOfferbook() {
        closeAndNavigateToHandler.accept(NavigationTarget.MU_SIG_OFFERBOOK);
    }

    void onKeyPressedWhileShowingOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, this::onShowOfferbook);
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, () -> {
        });
    }

    private void applyHeaderPaymentMethod() {
        List<PaymentMethod<?>> paymentMethods = model.getPaymentMethods();
        String bitcoinPaymentMethodsString = PaymentMethodSpecFormatter.fromPaymentMethods(paymentMethods);
        model.setHeaderPaymentMethod(bitcoinPaymentMethodsString);
        muSigReviewDataDisplay.setPaymentMethod(bitcoinPaymentMethodsString);
    }

    private void applyPriceDetails(DraftSnapshot snapshot) {
        PriceSpec priceSpec = snapshot.priceSpec();
        Optional<PriceQuote> marketPriceQuote = snapshot.marketPriceQuote();
        marketPriceQuote.ifPresent(quote -> model.setMarketPrice(quote.getValue()));
        String marketPriceAsString = marketPriceQuote.map(PriceFormatter::formatWithCode).orElseGet(() -> Res.get("data.na"));
        // The domain keeps the percentage as the authoritative floating value or the fixed
        // price's display deviation; both are what the review should show.
        Optional<Double> percentFromMarketPrice = Optional.of(snapshot.pricePercentage());
        double percent = snapshot.pricePercentage();
        if ((priceSpec instanceof FloatPriceSpec || priceSpec instanceof MarketPriceSpec) && percent == 0) {
            model.setPriceDetails(Res.get("muSig.offer.wizard.review.priceDetails"));
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

    private void verifyPaymentMethods(List<PaymentMethod<?>> paymentMethods) {
        if (paymentMethods == null || paymentMethods.isEmpty()) {
            throw new IllegalArgumentException("No payment methods provided");
        }

        boolean allFiat = paymentMethods.stream().allMatch(FiatPaymentMethod.class::isInstance);
        boolean allCrypto = paymentMethods.stream().allMatch(CryptoPaymentMethod.class::isInstance);
        if (!(allFiat || allCrypto)) {
            throw new IllegalArgumentException("All payment methods must be either fiat or crypto (no mixing).");
        }
    }

    private String calculateSecurityDeposit(Market market, Monetary baseSideMonetary, Monetary quoteSideMonetary) {
        double securityDepositAsPercent = model.getSecurityDepositAsPercent();
        Monetary securityDeposit = OfferAmountUtil.calculateSecurityDepositAsBTC(market,
                baseSideMonetary,
                quoteSideMonetary,
                securityDepositAsPercent);
        return OfferAmountFormatter.formatDepositAmountAsBTC(securityDeposit);
    }
}
