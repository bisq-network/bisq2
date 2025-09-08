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

package bisq.desktop.main.content.mu_sig.create_offer.review;

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.AccountUtils;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.common.observable.map.ReadOnlyObservableMap;
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
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.account.payment_method.PaymentMethodSpecFormatter;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.user.banned.RateLimitExceededException;
import bisq.user.banned.UserProfileBannedException;
import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class MuSigCreateOfferReviewController implements Controller {
    private final MuSigCreateOfferReviewModel model;
    @Getter
    private final MuSigCreateOfferReviewView view;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final PriceInput priceInput;
    private final MarketPriceService marketPriceService;
    private final MuSigReviewDataDisplay muSigReviewDataDisplay;
    private final MuSigService muSigService;
    private Pin errorMessagePin, peersErrorMessagePin;
    private UIScheduler timeoutScheduler;

    public MuSigCreateOfferReviewController(ServiceProvider serviceProvider,
                                            Consumer<Boolean> mainButtonsVisibleHandler,
                                            Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;

        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        muSigService = serviceProvider.getMuSigService();

        priceInput = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());
        muSigReviewDataDisplay = new MuSigReviewDataDisplay();
        model = new MuSigCreateOfferReviewModel();
        view = new MuSigCreateOfferReviewView(model, this, muSigReviewDataDisplay.getRoot());
    }

    public void setPaymentMethods(List<PaymentMethod<?>> paymentMethods) {
        if (paymentMethods != null) {
            resetSelectedPaymentMethod();
            model.setPaymentMethods(paymentMethods);
        }
    }

    public void setSelectedBisqEasyOffer(BisqEasyOffer selectedBisqEasyOffer) {
        if (selectedBisqEasyOffer != null) {
            resetSelectedPaymentMethod();
        }
    }

    public void setDirection(Direction direction) {
        model.setDirection(direction);
    }

    public void setMarket(Market market) {
        model.setMarket(market);
    }

    public void setDataForCreateOffer(Direction direction,
                                      Market market,
                                      ReadOnlyObservableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod,
                                      AmountSpec amountSpec,
                                      PriceSpec priceSpec) {

        List<PaymentMethod<?>> paymentMethods = new ArrayList<>();
        List<Account<?, ?>> accounts = new ArrayList<>();
        selectedAccountByPaymentMethod.entrySet().stream()
                .sorted(Comparator.comparing(o -> o.getKey().getPaymentRailName()))
                .forEach(e -> {
                    paymentMethods.add(e.getKey());
                    accounts.add(e.getValue());
                });

        model.setPaymentMethods(paymentMethods);
        model.setPaymentMethodDescription(
                paymentMethods.size() == 1
                        ? Res.get("muSig.createOffer.review.paymentMethod.description")
                        : Res.get("muSig.createOffer.review.paymentMethods.description")
        );
        model.setPaymentMethodsDisplayString(PaymentMethodSpecFormatter.fromPaymentMethods(paymentMethods));
        List<String> accountNames = accounts.stream()
                .map(Account::getAccountName)
                .collect(Collectors.toList());
        model.setPaymentMethodDetails(Joiner.on(", ").join(accountNames));

        verifyPaymentMethods(paymentMethods);

        String offerId = StringUtils.createUid();
        List<AccountOption> offerOptions = selectedAccountByPaymentMethod.entrySet().stream().map(entry -> {
            Account<?, ?> account = entry.getValue();
            AccountPayload<?> accountPayload = account.getAccountPayload();
            String saltedAccountId = OfferOptionUtil.createdSaltedAccountId(account.getId(), offerId);
            Optional<String> countryCode = AccountUtils.getCountryCode(accountPayload);
            List<String> acceptedCountryCodes = AccountUtils.getAcceptedCountryCodes(accountPayload);
            Optional<String> bankId = AccountUtils.getBankId(accountPayload);
            List<String> acceptedBanks = AccountUtils.getAcceptedBanks(accountPayload);
            return new AccountOption(account.getPaymentMethod(), saltedAccountId, countryCode, acceptedCountryCodes, bankId, acceptedBanks);
        }).collect(Collectors.toList());

        MuSigOffer offer = muSigService.createAndGetMuSigOffer(offerId,
                direction,
                market,
                amountSpec,
                priceSpec,
                paymentMethods,
                offerOptions);
        model.setOffer(offer);

        applyData(direction,
                market,
                amountSpec,
                priceSpec);
    }

    public void publishOffer() {
        MuSigOffer muSigOffer = model.getOffer();
        try {
            muSigService.publishOffer(muSigOffer).whenComplete((result, throwable) -> {
                if (throwable == null) {
                    UIThread.run(() -> {
                        model.getShowCreateOfferSuccess().set(true);
                        mainButtonsVisibleHandler.accept(false);
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
            UIThread.run(() -> new Popup().warning(Res.get("muSig.createOffer.rateLimitsExceeded.publish.warning")).show());
        }
    }

    // direction is from user perspective not offer direction
    private void applyData(Direction direction,
                           Market market,
                           AmountSpec amountSpec,
                           PriceSpec priceSpec) {
        String marketCodes = market.getMarketCodes();

        model.setPriceSpec(priceSpec);
        priceInput.setMarket(market);
        priceInput.setDescription(Res.get("bisqEasy.tradeWizard.review.priceDescription.taker", marketCodes));

        Optional<PriceQuote> priceQuote = PriceUtil.findQuote(marketPriceService, priceSpec, market);
        priceQuote.ifPresent(priceInput::setQuote);
        String formattedPrice = priceQuote
                .map(PriceFormatter::format)
                .orElse("");
        String codes = priceQuote.map(e -> e.getMarket().getMarketCodes()).orElse("");
        model.setPrice(Res.get("bisqEasy.tradeWizard.review.price", formattedPrice, codes));

        applyPriceDetails(model.getPriceSpec(), market);

        model.setRangeAmount(amountSpec instanceof RangeAmountSpec);
        String currentToSendMinAmount = null, currentToReceiveMinAmount = null,
                currentToReceiveMaxOrFixedAmount, currentToSendMaxOrFixedAmount,
                toSendAmountDescription, toSendCode, toReceiveAmountDescription, toReceiveCode;
        if (model.isRangeAmount()) {
            Monetary minBaseSideAmount = OfferAmountUtil.findBaseSideMinAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMinBaseSideAmount(minBaseSideAmount);
            Monetary maxBaseSideAmount = OfferAmountUtil.findBaseSideMaxAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMaxBaseSideAmount(maxBaseSideAmount);

            Monetary minQuoteSideAmount = OfferAmountUtil.findQuoteSideMinAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMinQuoteSideAmount(minQuoteSideAmount);
            Monetary maxQuoteSideAmount = OfferAmountUtil.findQuoteSideMaxAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMaxQuoteSideAmount(maxQuoteSideAmount);

            String formattedMinQuoteAmount = AmountFormatter.formatQuoteAmount(minQuoteSideAmount);
            String formattedMinBaseAmount = AmountFormatter.formatBaseAmount(minBaseSideAmount);
            String formattedMaxQuoteAmount = AmountFormatter.formatQuoteAmount(maxQuoteSideAmount);
            String formattedMaxBaseAmount = AmountFormatter.formatBaseAmount(maxBaseSideAmount);
            if (direction.isSell()) {
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
        } else {
            Monetary fixBaseSideAmount = OfferAmountUtil.findBaseSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setFixBaseSideAmount(fixBaseSideAmount);
            String formattedBaseAmount = AmountFormatter.formatBaseAmount(fixBaseSideAmount);

            Monetary fixQuoteSideAmount = OfferAmountUtil.findQuoteSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setFixQuoteSideAmount(fixQuoteSideAmount);
            String formattedQuoteAmount = AmountFormatter.formatQuoteAmount(fixQuoteSideAmount);

            if (direction.isSell()) {
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
        }

        model.setHeadline(Res.get("bisqEasy.tradeWizard.review.headline.maker"));
        model.setDetailsHeadline(Res.get("bisqEasy.tradeWizard.review.detailsHeadline.maker").toUpperCase());

        model.setPriceDescription(Res.get("bisqEasy.tradeWizard.review.priceDescription.maker"));
        if (direction.isSell()) {
            toSendAmountDescription = Res.get("bisqEasy.tradeWizard.review.toSend");
        } else {
            toSendAmountDescription = Res.get("bisqEasy.tradeWizard.review.toPay");
        }
        toReceiveAmountDescription = Res.get("bisqEasy.tradeWizard.review.toReceive");

        String directionString = String.format("%s %s",
                Res.get(direction.isSell() ? "offer.sell" : "offer.buy").toUpperCase(),
                model.getMarket().getBaseCurrencyDisplayName());

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
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        model.getShowCreateOfferSuccess().set(false);
        Direction direction = model.getOffer().getDirection();
        if (direction.isSell()) {
            model.setFee(Res.get("bisqEasy.tradeWizard.review.sellerPaysMinerFee"));
            model.setFeeDetails(Res.get("bisqEasy.tradeWizard.review.noTradeFeesLong"));
        } else {
            model.setFee(Res.get("bisqEasy.tradeWizard.review.noTradeFees"));
            model.setFeeDetails(Res.get("bisqEasy.tradeWizard.review.sellerPaysMinerFeeLong"));
        }
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

    void onShowOfferbook() {
        closeAndNavigateToHandler.accept(NavigationTarget.MU_SIG_OFFERBOOK);
    }

    private void resetSelectedPaymentMethod() {
        model.setTakersSelectedPaymentMethod(null);
    }

    private void applyHeaderPaymentMethod() {
        List<PaymentMethod<?>> paymentMethods = model.getPaymentMethods();
        String bitcoinPaymentMethodsString = PaymentMethodSpecFormatter.fromPaymentMethods(paymentMethods);
        model.setHeaderPaymentMethod(bitcoinPaymentMethodsString);
        muSigReviewDataDisplay.setPaymentMethod(bitcoinPaymentMethodsString);
    }

    private void applyPriceDetails(PriceSpec priceSpec, Market market) {
        Optional<MarketPrice> marketPrice = marketPriceService.findMarketPrice(market);
        marketPrice.ifPresent(price -> model.setMarketPrice(price.getPriceQuote().getValue()));
        Optional<PriceQuote> marketPriceQuote = marketPriceService.findMarketPrice(market).map(MarketPrice::getPriceQuote);
        String marketPriceAsString = marketPriceQuote.map(PriceFormatter::formatWithCode).orElseGet(() -> Res.get("data.na"));
        Optional<Double> percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, priceSpec, market);
        double percent = percentFromMarketPrice.orElse(0d);
        if ((priceSpec instanceof FloatPriceSpec || priceSpec instanceof MarketPriceSpec) && percent == 0) {
            model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails"));
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

    private void verifyPaymentMethods(List<PaymentMethod<?>> paymentMethods) {
        if (paymentMethods == null || paymentMethods.isEmpty()) {
            throw new IllegalArgumentException("No payment methods provided");
        }
        boolean allFiat = paymentMethods.stream().allMatch(pm -> pm instanceof FiatPaymentMethod);
        boolean allCrypto = paymentMethods.stream().allMatch(pm -> pm instanceof CryptoPaymentMethod);
        if (!(allFiat || allCrypto)) {
            throw new IllegalArgumentException("All payment methods must be either fiat or crypto (no mixing).");
        }
    }
}
