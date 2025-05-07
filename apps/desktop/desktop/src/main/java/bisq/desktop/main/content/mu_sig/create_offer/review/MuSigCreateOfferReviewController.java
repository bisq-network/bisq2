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

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.bisq_easy.components.PriceInput;
import bisq.desktop.main.content.bisq_easy.components.ReviewDataDisplay;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.OfferOption;
import bisq.offer.payment_method.PaymentMethodSpecFormatter;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.SettingsService;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MuSigCreateOfferReviewController implements Controller {
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private static final String DASH_SYMBOL = "\u2013"; // Unicode for "–"

    private final MuSigCreateOfferReviewModel model;
    @Getter
    private final MuSigCreateOfferReviewView view;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final PriceInput priceInput;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final BannedUserService bannedUserService;
    private final SettingsService settingsService;
    private final ReviewDataDisplay reviewDataDisplay;
    private final MediationRequestService mediationRequestService;
    private final MuSigService muSigService;
    private Pin errorMessagePin, peersErrorMessagePin;
    private UIScheduler timeoutScheduler;

    public MuSigCreateOfferReviewController(ServiceProvider serviceProvider,
                                            Consumer<Boolean> mainButtonsVisibleHandler,
                                            Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;

        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        settingsService = serviceProvider.getSettingsService();
        muSigService = serviceProvider.getMuSigService();
        mediationRequestService = serviceProvider.getSupportService().getMediationRequestService();

        priceInput = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());
        reviewDataDisplay = new ReviewDataDisplay();
        model = new MuSigCreateOfferReviewModel();
        view = new MuSigCreateOfferReviewView(model, this, reviewDataDisplay.getRoot());
    }

    public void setBitcoinPaymentMethods(List<BitcoinPaymentMethod> bitcoinPaymentMethods) {
        if (bitcoinPaymentMethods != null) {
            resetSelectedPaymentMethod();
        }
    }

    public void setFiatPaymentMethods(List<FiatPaymentMethod> fiatPaymentMethods) {
        if (fiatPaymentMethods != null) {
            resetSelectedPaymentMethod();
        }
    }

    public void setSelectedBisqEasyOffer(BisqEasyOffer selectedBisqEasyOffer) {
        if (selectedBisqEasyOffer != null) {
            resetSelectedPaymentMethod();
        }
    }

    public void setDataForCreateOffer(Direction direction,
                                      Market market,
                                      List<BitcoinPaymentMethod> bitcoinPaymentMethods,
                                      List<FiatPaymentMethod> fiatPaymentMethods,
                                      AmountSpec amountSpec,
                                      PriceSpec priceSpec) {
        checkArgument(!bitcoinPaymentMethods.isEmpty(), "bitcoinPaymentMethods must not be empty");
        checkArgument(!fiatPaymentMethods.isEmpty(), "fiatPaymentMethods must not be empty");
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();

        List<OfferOption> offerOptions = List.of();
        MuSigOffer offer = muSigService.createAndGetMuSigOffer(direction,
                market,
                amountSpec,
                priceSpec,
                fiatPaymentMethods,
                offerOptions);
        model.setOffer(offer);

        applyData(direction,
                market,
                bitcoinPaymentMethods,
                fiatPaymentMethods,
                amountSpec,
                priceSpec);
    }

    public void publishOffer() {
        muSigService.publishOffer(model.getOffer()).whenComplete((result, throwable) -> {
            if (throwable == null) {
                UIThread.run(() -> {
                    model.getShowCreateOfferSuccess().set(true);
                    mainButtonsVisibleHandler.accept(false);
                });
                result.forEach(future -> {
                    future.whenComplete((res, t) -> {
                        if (t == null) {
                            log.error("Offer published. result={}", res);
                        } else {
                            log.error("Offer publishing failed with", throwable);
                        }
                    });
                });
            } else {
                log.error("Offer publishing failed", throwable);
            }
        });
    }

    // direction is from user perspective not offer direction
    private void applyData(Direction direction,
                           Market market,
                           List<BitcoinPaymentMethod> bitcoinPaymentMethods,
                           List<FiatPaymentMethod> fiatPaymentMethods,
                           AmountSpec amountSpec,
                           PriceSpec priceSpec) {
        String marketCodes = market.getMarketCodes();

        model.setBitcoinPaymentMethods(bitcoinPaymentMethods);
        model.setFiatPaymentMethods(fiatPaymentMethods);
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

        String toSendAmountDescription, toSendAmount, toSendCode, toReceiveAmountDescription, toReceiveAmount, toReceiveCode;
        boolean isRangeAmount = amountSpec instanceof RangeAmountSpec;
        if (isRangeAmount) {
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
                toSendAmount = formattedMinBaseAmount + " " + DASH_SYMBOL + " " + formattedMaxBaseAmount;
                toSendCode = maxBaseSideAmount.getCode();
                toReceiveAmount = formattedMinQuoteAmount + " " + DASH_SYMBOL + " " + formattedMaxQuoteAmount;
                toReceiveCode = maxQuoteSideAmount.getCode();
            } else {
                toSendAmount = formattedMinQuoteAmount + " " + DASH_SYMBOL + " " + formattedMaxQuoteAmount;
                toSendCode = maxQuoteSideAmount.getCode();
                toReceiveAmount = formattedMinBaseAmount + " " + DASH_SYMBOL + " " + formattedMaxBaseAmount;
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
                toSendAmount = formattedBaseAmount;
                toSendCode = fixBaseSideAmount.getCode();
                toReceiveAmount = formattedQuoteAmount;
                toReceiveCode = fixQuoteSideAmount.getCode();
            } else {
                toSendAmount = formattedQuoteAmount;
                toSendCode = fixQuoteSideAmount.getCode();
                toReceiveAmount = formattedBaseAmount;
                toReceiveCode = fixBaseSideAmount.getCode();
            }
        }

        model.setHeadline(Res.get("bisqEasy.tradeWizard.review.headline.maker"));
        model.setDetailsHeadline(Res.get("bisqEasy.tradeWizard.review.detailsHeadline.maker").toUpperCase());
        model.setBitcoinPaymentMethodDescription(
                bitcoinPaymentMethods.size() == 1
                        ? Res.get("bisqEasy.tradeWizard.review.paymentMethodDescription.btc")
                        : Res.get("bisqEasy.tradeWizard.review.paymentMethodDescriptions.btc.maker")
        );
        model.setFiatPaymentMethodDescription(
                fiatPaymentMethods.size() == 1
                        ? Res.get("bisqEasy.tradeWizard.review.paymentMethodDescription.fiat")
                        : Res.get("bisqEasy.tradeWizard.review.paymentMethodDescriptions.fiat.maker")
        );
        model.setBitcoinPaymentMethod(PaymentMethodSpecFormatter.fromPaymentMethods(bitcoinPaymentMethods));
        model.setFiatPaymentMethod(PaymentMethodSpecFormatter.fromPaymentMethods(fiatPaymentMethods));
        model.setPriceDescription(Res.get("bisqEasy.tradeWizard.review.priceDescription.maker"));
        if (direction.isSell()) {
            toSendAmountDescription = Res.get("bisqEasy.tradeWizard.review.toSend");
        } else {
            toSendAmountDescription = Res.get("bisqEasy.tradeWizard.review.toPay");
        }
        toReceiveAmountDescription = Res.get("bisqEasy.tradeWizard.review.toReceive");

        applyHeaderBitcoinPaymentMethod();
        applyHeaderFiatPaymentMethod();

        model.setRangeAmount(isRangeAmount);
        reviewDataDisplay.setRangeAmount(isRangeAmount);
        reviewDataDisplay.setDirection(Res.get("bisqEasy.tradeWizard.review.direction", Res.get(direction.isSell() ? "offer.sell" : "offer.buy").toUpperCase()));
        reviewDataDisplay.setToSendAmountDescription(toSendAmountDescription.toUpperCase());
        reviewDataDisplay.setToSendAmount(toSendAmount);
        reviewDataDisplay.setToSendCode(toSendCode);
        reviewDataDisplay.setToReceiveAmountDescription(toReceiveAmountDescription.toUpperCase());
        reviewDataDisplay.setToReceiveAmount(toReceiveAmount);
        reviewDataDisplay.setToReceiveCode(toReceiveCode);
        reviewDataDisplay.setBitcoinPaymentMethodDescription(model.getBitcoinPaymentMethodDescription().toUpperCase());
        reviewDataDisplay.setFiatPaymentMethodDescription(model.getFiatPaymentMethodDescription().toUpperCase());
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        model.getShowCreateOfferSuccess().set(false);
        Direction direction = model.getOffer().getDirection();
        boolean isMainChain = model.getBitcoinPaymentMethods().stream().anyMatch(e -> e.getPaymentRail() == BitcoinPaymentRail.MAIN_CHAIN);
        model.setFeeDetailsVisible(isMainChain);
        if (direction.isSell()) {
            if (isMainChain) {
                model.setFee(Res.get("bisqEasy.tradeWizard.review.sellerPaysMinerFee"));
                model.setFeeDetails(Res.get("bisqEasy.tradeWizard.review.noTradeFeesLong"));
            } else {
                model.setFee(Res.get("bisqEasy.tradeWizard.review.noTradeFees"));
            }
        } else {
            if (isMainChain) {
                model.setFee(Res.get("bisqEasy.tradeWizard.review.noTradeFees"));
                model.setFeeDetails(Res.get("bisqEasy.tradeWizard.review.sellerPaysMinerFeeLong"));
            } else {
                model.setFee(Res.get("bisqEasy.tradeWizard.review.noTradeFees"));
            }
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
        closeAndNavigateToHandler.accept(NavigationTarget.MU_SIG_OFFERBOOK_BUY);
    }

    void onSelectBitcoinPaymentMethod(BitcoinPaymentMethod paymentMethod) {
        model.setTakersSelectedBitcoinPaymentMethod(paymentMethod);
        applyHeaderBitcoinPaymentMethod();
    }

    void onSelectFiatPaymentMethod(FiatPaymentMethod paymentMethod) {
        model.setTakersSelectedFiatPaymentMethod(paymentMethod);
        applyHeaderFiatPaymentMethod();
    }

    private void resetSelectedPaymentMethod() {
        model.setTakersSelectedBitcoinPaymentMethod(null);
        model.setTakersSelectedFiatPaymentMethod(null);
    }

    private void applyHeaderBitcoinPaymentMethod() {
        List<BitcoinPaymentMethod> bitcoinPaymentMethods = model.getBitcoinPaymentMethods();
        String bitcoinPaymentMethodsString;
        if (bitcoinPaymentMethods.size() > 2) {
            bitcoinPaymentMethodsString = PaymentMethodSpecFormatter.fromPaymentMethods(bitcoinPaymentMethods.stream()
                    .limit(2)
                    .collect(Collectors.toList())) + ",...";
        } else {
            bitcoinPaymentMethodsString = PaymentMethodSpecFormatter.fromPaymentMethods(bitcoinPaymentMethods);
        }
        bitcoinPaymentMethodsString = StringUtils.truncate(bitcoinPaymentMethodsString, 40);
        model.setHeaderBitcoinPaymentMethod(bitcoinPaymentMethodsString);
        reviewDataDisplay.setBitcoinPaymentMethod(bitcoinPaymentMethodsString);
    }

    private void applyHeaderFiatPaymentMethod() {
        List<FiatPaymentMethod> bitcoinPaymentMethods = model.getFiatPaymentMethods();
        String bitcoinPaymentMethodsString;
        if (bitcoinPaymentMethods.size() > 2) {
            bitcoinPaymentMethodsString = PaymentMethodSpecFormatter.fromPaymentMethods(bitcoinPaymentMethods.stream()
                    .limit(2)
                    .collect(Collectors.toList())) + ",...";
        } else {
            bitcoinPaymentMethodsString = PaymentMethodSpecFormatter.fromPaymentMethods(bitcoinPaymentMethods);
        }
        bitcoinPaymentMethodsString = StringUtils.truncate(bitcoinPaymentMethodsString, 40);
        model.setHeaderFiatPaymentMethod(bitcoinPaymentMethodsString);
        reviewDataDisplay.setFiatPaymentMethod(bitcoinPaymentMethodsString);
    }

    private void applyPriceDetails(PriceSpec priceSpec, Market market) {
        Optional<MarketPrice> marketPrice = marketPriceService.findMarketPrice(market);
        marketPrice.ifPresent(price -> model.setMarketPrice(price.getPriceQuote().getValue()));
        Optional<PriceQuote> marketPriceQuote = marketPriceService.findMarketPrice(market).map(MarketPrice::getPriceQuote);
        String marketPriceAsString = marketPriceQuote.map(PriceFormatter::formatWithCode).orElse(Res.get("data.na"));
        Optional<Double> percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, priceSpec, market);
        double percent = percentFromMarketPrice.orElse(0d);
        if ((priceSpec instanceof FloatPriceSpec || priceSpec instanceof MarketPriceSpec) && percent == 0) {
            model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails"));
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
}
