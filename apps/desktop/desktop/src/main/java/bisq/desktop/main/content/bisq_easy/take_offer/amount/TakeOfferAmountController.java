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

package bisq.desktop.main.content.bisq_easy.take_offer.amount;

import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.PriceUtil;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class TakeOfferAmountController implements Controller {
    private final TakeOfferAmountModel model;
    @Getter
    private final TakeOfferAmountView view;
    private final AmountComponent amountComponent;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final ReputationService reputationService;
    private Subscription baseSideAmountPin, quoteSideAmountPin;

    public TakeOfferAmountController(ServiceProvider serviceProvider) {
        model = new TakeOfferAmountModel();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        amountComponent = new AmountComponent(serviceProvider, true);
        view = new TakeOfferAmountView(model, this, amountComponent.getView().getRoot());
    }

    public void init(BisqEasyOffer bisqEasyOffer, Optional<Monetary> takerAsSellersMaxAllowedAmount) {
        model.setBisqEasyOffer(bisqEasyOffer);

        Direction takersDirection = bisqEasyOffer.getTakersDirection();
        model.setHeadline(takersDirection.isBuy() ? Res.get("bisqEasy.takeOffer.amount.headline.buyer") : Res.get("bisqEasy.takeOffer.amount.headline.seller"));
        amountComponent.setDirection(takersDirection);
        Market market = bisqEasyOffer.getMarket();
        amountComponent.setMarket(market);

        PriceUtil.findQuote(marketPriceService, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket())
                .ifPresent(amountComponent::setQuote);

        Optional<Monetary> optionalQuoteSideMinOrFixedAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, bisqEasyOffer);
        Optional<Monetary> optionalQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer);

        if (optionalQuoteSideMinOrFixedAmount.isPresent() && takerAsSellersMaxAllowedAmount.isPresent()) {
            //todo
            Monetary maxAmount = takerAsSellersMaxAllowedAmount.get();
            Monetary reputationBasedQuoteSideAmount = takerAsSellersMaxAllowedAmount.get();
            amountComponent.setMinMaxRange(optionalQuoteSideMinOrFixedAmount.get(), maxAmount);
            amountComponent.setReputationBasedQuoteSideAmount(reputationBasedQuoteSideAmount);
            applyQuoteSideMinMaxRange(optionalQuoteSideMinOrFixedAmount.get(), maxAmount);

            long sellersScore = reputationService.getReputationScore(userIdentityService.getSelectedUserIdentity().getUserProfile()).getTotalScore();
            amountComponent.setDescription(Res.get("bisqEasy.takeOffer.amount.description.limitedByTakersReputation",
                    sellersScore,
                    OfferAmountFormatter.formatQuoteSideMinAmount(marketPriceService, bisqEasyOffer, false),
                    AmountFormatter.formatAmountWithCode(maxAmount)));
        } else if (optionalQuoteSideMinOrFixedAmount.isPresent() && optionalQuoteSideMaxOrFixedAmount.isPresent()) {
            //todo
            Monetary maxAmount = optionalQuoteSideMaxOrFixedAmount.get();
            Monetary reputationBasedQuoteSideAmount = optionalQuoteSideMaxOrFixedAmount.get();
            applyQuoteSideMinMaxRange(optionalQuoteSideMinOrFixedAmount.get(), maxAmount);

            amountComponent.setDescription(Res.get("bisqEasy.takeOffer.amount.description",
                    OfferAmountFormatter.formatQuoteSideMinAmount(marketPriceService, bisqEasyOffer, false),
                    AmountFormatter.formatAmountWithCode(maxAmount)));
        } else {
            log.error("optionalQuoteSideMinOrFixedAmount or optionalQuoteSideMaxOrFixedAmount is not present");
        }

        String btcAmount = takersDirection.isBuy()
                ? Res.get("bisqEasy.component.amount.baseSide.tooltip.buyer.btcAmount")
                : Res.get("bisqEasy.component.amount.baseSide.tooltip.seller.btcAmount");
        Optional<String> priceQuoteOptional = PriceUtil.findQuote(marketPriceService, model.getBisqEasyOffer())
                .map(priceQuote -> "\n" + Res.get("bisqEasy.component.amount.baseSide.tooltip.taker.offerPrice", PriceFormatter.formatWithCode(priceQuote)));
        priceQuoteOptional.ifPresent(priceQuote -> amountComponent.setTooltip(String.format("%s%s", btcAmount, priceQuote)));
    }

    public ReadOnlyObjectProperty<Monetary> getTakersQuoteSideAmount() {
        return model.getTakersQuoteSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getTakersBaseSideAmount() {
        return model.getTakersBaseSideAmount();
    }

    @Override
    public void onActivate() {
        baseSideAmountPin = EasyBind.subscribe(amountComponent.getBaseSideAmount(),
                amount -> model.getTakersBaseSideAmount().set(amount));
        quoteSideAmountPin = EasyBind.subscribe(amountComponent.getQuoteSideAmount(),
                amount -> {
                    model.getTakersQuoteSideAmount().set(amount);
                    if (amount != null) {
                        maxOrFixedQuoteSideAmountChanged(amount);
                    }
                });
    }

    @Override
    public void onDeactivate() {
        baseSideAmountPin.unsubscribe();
        quoteSideAmountPin.unsubscribe();
        model.getIsWarningIconVisible().set(false);
        model.getIsAmountLimitInfoOverlayVisible().set(false);
    }

    void onSetReputationBasedAmount() {
        amountComponent.setQuoteSideAmount(amountComponent.getReputationBasedQuoteSideAmount());
    }

    void onShowAmountLimitInfoOverlay() {
        model.getIsAmountLimitInfoOverlayVisible().set(true);
    }

    void onCloseAmountLimitInfoOverlay() {
        model.getIsAmountLimitInfoOverlayVisible().set(false);
    }

    void onOpenWiki(String url) {
        Browser.open(url);
    }

    private void applyQuoteSideMinMaxRange(Monetary minRangeValue, Monetary maxRangeValue) {
        amountComponent.setMinMaxRange(minRangeValue, maxRangeValue);

        BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
        if (bisqEasyOffer.getTakersDirection().isBuy()) {
            model.setAmountLimitInfoLink(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.learnMore"));
            model.setLinkToWikiText(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.overlay.linkToWikiText"));
            model.getAmountLimitInfoAmount().set("");


            long sellersReputationScore = reputationService.getReputationScore(bisqEasyOffer.getMakersUserProfileId()).getTotalScore();
            Monetary reputationBasedQuoteSideAmount = BisqEasyTradeAmountLimits.getMaxQuoteSideTradeAmount(marketPriceService, bisqEasyOffer.getMarket(), sellersReputationScore)
                    .orElse(Fiat.fromFaceValue(0, "USD"));
            if (reputationBasedQuoteSideAmount.isLessThan(maxRangeValue)) {
                model.getIsAmountLimitInfoVisible().set(true);
                amountComponent.setReputationBasedQuoteSideAmount(reputationBasedQuoteSideAmount);
                String formattedAmount = AmountFormatter.formatAmountWithCode(reputationBasedQuoteSideAmount);
                model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.overlay.info", sellersReputationScore, formattedAmount) + "\n\n");
                if (reputationBasedQuoteSideAmount.isLessThan(minRangeValue)) {
                    // Min amount not covered by security from reputation score
                    model.getAmountLimitInfoLeft().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.tooHighMin", sellersReputationScore));
                    model.getAmountLimitInfoAmount().set("");
                } else {
                    // Max amount not covered by security from reputation score
                    model.getAmountLimitInfoLeft().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.tooHighMax", sellersReputationScore));
                    model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfoAmount", formattedAmount));
                }
            } else {
                model.getIsAmountLimitInfoVisible().set(false);
            }


           /* long highestScore = reputationService.getScoreByUserProfileId().entrySet().stream()
                    .filter(e -> userIdentityService.findUserIdentity(e.getKey()).isEmpty())
                    .mapToLong(Map.Entry::getValue)
                    .max()
                    .orElse(0L);
            Monetary highestPossibleUsdAmount = BisqEasyTradeAmountLimits.getUsdAmountFromReputationScore(highestScore);

            amountComponent.setReputationBasedQuoteSideAmount(highestPossibleUsdAmount);
            long rangeMidValue = minRangeValue.getValue() + (maxRangeValue.getValue() - minRangeValue.getValue()) / 2;
            // For buyers we show the mid-range amount if there is a highestPossibleUsdAmount > rangeMidValue
            if (highestPossibleUsdAmount.getValue() > rangeMidValue &&
                    rangeMidValue > BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION.getValue()) {
                amountComponent.setQuoteSideAmount(Fiat.fromValue(rangeMidValue, "USD"));
            } else {
                applyReputationBasedQuoteSideAmount();
            }*/
        } else {
            model.setLinkToWikiText(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.linkToWikiText"));
            String myProfileId = userIdentityService.getSelectedUserIdentity().getUserProfile().getId();
            long myReputationScore = reputationService.getReputationScore(myProfileId).getTotalScore();
            BisqEasyTradeAmountLimits.getMaxQuoteSideTradeAmount(marketPriceService, bisqEasyOffer.getMarket(), myReputationScore)
                    .ifPresent(reputationBasedQuoteSideAmount -> {
                        amountComponent.setMinMaxRange(minRangeValue, maxRangeValue);
                        amountComponent.setReputationBasedQuoteSideAmount(reputationBasedQuoteSideAmount);
                        String formattedAmount = AmountFormatter.formatAmountWithCode(reputationBasedQuoteSideAmount);
                        model.getAmountLimitInfoLeft().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfoLeft", myReputationScore));
                        model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfoAmount", formattedAmount));
                        model.setAmountLimitInfoLink(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.link"));
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.info", myReputationScore, formattedAmount));
                    });
            applyReputationBasedQuoteSideAmount();
        }
    }

    private void applyReputationBasedQuoteSideAmount() {
        // Reduce by 1 USD to avoid rounding issues, but use at least 25 USD (MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION)
        long value = amountComponent.getReputationBasedQuoteSideAmount().getValue() - 10000;
        value = Math.max(BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION.getValue(), value);
        amountComponent.setQuoteSideAmount(Fiat.fromValue(value, "USD"));
    }

    private void maxOrFixedQuoteSideAmountChanged(Monetary value) {
        if (amountComponent.getReputationBasedQuoteSideAmount() == null) {
            return;
        }
        double reputationBasedAmountFaceValue = Monetary.valueToFaceValue(amountComponent.getReputationBasedQuoteSideAmount(), 0);
        double faceValue = Monetary.valueToFaceValue(value, 0);
        model.getIsWarningIconVisible().set(faceValue > reputationBasedAmountFaceValue);

        long highestScore = reputationService.getScoreByUserProfileId().entrySet().stream()
                .filter(e -> userIdentityService.findUserIdentity(e.getKey()).isEmpty())
                .mapToLong(Map.Entry::getValue)
                .max()
                .orElse(0L);
       /* Monetary highestPossibleUsdAmount = BisqEasyTradeAmountLimits.getUsdAmountFromReputationScore(highestScore);
        // We reduce by 1 USD to avoid rounding issues
        Fiat oneUsd = Fiat.fromFaceValue(1d, "USD");
        highestPossibleUsdAmount = Monetary.from(highestPossibleUsdAmount, highestPossibleUsdAmount.getValue() - oneUsd.getValue());*/
        Market usdBitcoinMarket = MarketRepository.getUSDBitcoinMarket();
        long requiredReputationScore = BisqEasyTradeAmountLimits.findRequiredReputationScoreByFiatAmount(marketPriceService, usdBitcoinMarket, value).orElse(0L);
        long numPotentialTakers = reputationService.getScoreByUserProfileId().entrySet().stream()
                .filter(e -> userIdentityService.findUserIdentity(e.getKey()).isEmpty())
                .filter(e -> e.getValue() >= requiredReputationScore || requiredReputationScore <= BisqEasyTradeAmountLimits.MIN_REPUTAION_SCORE)
                .count();
        if (model.getBisqEasyOffer().getTakersDirection().isBuy()) {
            // model.getAmountLimitInfoLeft().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfoLeft", numPotentialTakers, AmountFormatter.formatAmountWithCode(value)));
            // model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info", AmountFormatter.formatAmountWithCode(value), requiredReputationScore));
        }
    }
}
