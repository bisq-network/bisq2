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
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.bisq_easy.components.amount_selection.AmountSelectionController;
import bisq.i18n.Res;
import bisq.offer.Direction;
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

import java.util.Optional;

import static bisq.bisq_easy.BisqEasyTradeAmountLimits.*;
import static bisq.presentation.formatters.AmountFormatter.formatAmountWithCode;

@Slf4j
public class TakeOfferAmountController implements Controller {
    private final TakeOfferAmountModel model;
    @Getter
    private final TakeOfferAmountView view;
    private final AmountSelectionController amountSelectionController;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final ReputationService reputationService;
    private Subscription baseSideAmountPin, quoteSideAmountPin;

    public TakeOfferAmountController(ServiceProvider serviceProvider) {
        model = new TakeOfferAmountModel();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        amountSelectionController = new AmountSelectionController(serviceProvider);
        view = new TakeOfferAmountView(model, this, amountSelectionController.getView().getRoot());
    }

    public void init(BisqEasyOffer bisqEasyOffer, Optional<Monetary> optionalReputationBasedQuoteSideAmount) {
        model.setBisqEasyOffer(bisqEasyOffer);

        Direction takersDirection = bisqEasyOffer.getTakersDirection();
        model.setHeadline(takersDirection.isBuy() ? Res.get("bisqEasy.takeOffer.amount.headline.buyer") : Res.get("bisqEasy.takeOffer.amount.headline.seller"));
        amountSelectionController.setDirection(takersDirection);
        Market market = bisqEasyOffer.getMarket();
        amountSelectionController.setMarket(market);

        PriceUtil.findQuote(marketPriceService, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket())
                .ifPresent(amountSelectionController::setQuote);

        Optional<Monetary> OptionalQuoteSideMinAmount = OfferAmountUtil.findQuoteSideMinAmount(marketPriceService, bisqEasyOffer);
        if (OptionalQuoteSideMinAmount.isPresent()) {
            Monetary quoteSideMinAmount = OptionalQuoteSideMinAmount.get().round(0);
            Monetary offersQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer).orElseThrow().round(0);
            String formattedMinAmount = AmountFormatter.formatAmount(quoteSideMinAmount);
            Monetary maxAmount;
            if (optionalReputationBasedQuoteSideAmount.isPresent()) {
                // Range amounts seller case with provided reputationBasedQuoteSideAmount
                Monetary reputationBasedQuoteSideAmount = optionalReputationBasedQuoteSideAmount.get().round(0);
                maxAmount = reputationBasedQuoteSideAmount.isGreaterThan(offersQuoteSideMaxOrFixedAmount)
                        ? offersQuoteSideMaxOrFixedAmount
                        : reputationBasedQuoteSideAmount;
                amountSelectionController.setRightMarkerQuoteSideValue(maxAmount);
                applyQuoteSideMinMaxRange(quoteSideMinAmount, maxAmount);

                long sellersScore = reputationService.getReputationScore(userIdentityService.getSelectedUserIdentity().getUserProfile()).getTotalScore();
                // TODO: Move this outside of the component
                amountSelectionController.setDescription(Res.get("bisqEasy.takeOffer.amount.description.limitedByTakersReputation",
                        sellersScore,
                        formattedMinAmount,
                        AmountFormatter.formatAmountWithCode(maxAmount)));
            } else {
                // Range amounts buyer case
                maxAmount = offersQuoteSideMaxOrFixedAmount;
                applyQuoteSideMinMaxRange(quoteSideMinAmount, maxAmount);
                // TODO: Move this outside of the component
                amountSelectionController.setDescription(Res.get("bisqEasy.takeOffer.amount.description",
                        formattedMinAmount,
                        AmountFormatter.formatAmountWithCode(maxAmount)));
            }
            String btcAmount = takersDirection.isBuy()
                    ? Res.get("bisqEasy.component.amount.baseSide.tooltip.buyer.btcAmount")
                    : Res.get("bisqEasy.component.amount.baseSide.tooltip.seller.btcAmount");
            Optional<String> priceQuoteOptional = PriceUtil.findQuote(marketPriceService, model.getBisqEasyOffer())
                    .map(priceQuote -> "\n" + Res.get("bisqEasy.component.amount.baseSide.tooltip.taker.offerPrice", PriceFormatter.formatWithCode(priceQuote)));
            priceQuoteOptional.ifPresent(priceQuote -> amountSelectionController.setTooltip(String.format("%s%s", btcAmount, priceQuote)));
        }
    }

    public ReadOnlyObjectProperty<Monetary> getTakersQuoteSideAmount() {
        return model.getTakersQuoteSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getTakersBaseSideAmount() {
        return model.getTakersBaseSideAmount();
    }

    @Override
    public void onActivate() {
        baseSideAmountPin = EasyBind.subscribe(amountSelectionController.getMaxOrFixedBaseSideAmount(),
                amount -> model.getTakersBaseSideAmount().set(amount));
        quoteSideAmountPin = EasyBind.subscribe(amountSelectionController.getMaxOrFixedQuoteSideAmount(),
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
        view.getRoot().setOnKeyPressed(null);
        model.getIsWarningIconVisible().set(false);
        model.getIsAmountLimitInfoOverlayVisible().set(false);
        model.setSellersReputationBasedQuoteSideAmount(null);
    }

    void onSetReputationBasedAmount() {
        amountSelectionController.setMaxOrFixedQuoteSideAmount(amountSelectionController.getRightMarkerQuoteSideValue().round(0));
    }

    void onShowAmountLimitInfoOverlay() {
        model.getIsAmountLimitInfoOverlayVisible().set(true);
        view.getRoot().setOnKeyPressed(keyEvent -> {
            KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
            });
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseAmountLimitInfoOverlay);
        });
    }

    void onCloseAmountLimitInfoOverlay() {
        model.getIsAmountLimitInfoOverlayVisible().set(false);
        view.getRoot().setOnKeyPressed(null);
    }

    void onOpenWiki(String url) {
        Browser.open(url);
    }

    private void applyQuoteSideMinMaxRange(Monetary minRangeValue, Monetary maxRangeValue) {
        amountSelectionController.setMinMaxRange(minRangeValue, maxRangeValue);

        BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
        Market market = bisqEasyOffer.getMarket();
        if (bisqEasyOffer.getTakersDirection().isBuy()) {
            // Buyer case
            model.setAmountLimitInfoLink(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.learnMore"));
            model.setLinkToWikiText(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.overlay.linkToWikiText"));
            model.getAmountLimitInfoAmount().set("");

            if (model.getSellersReputationBasedQuoteSideAmount() == null) {
                long sellersReputationScore = reputationService.getReputationScore(bisqEasyOffer.getMakersUserProfileId()).getTotalScore();
                model.setSellersReputationScore(sellersReputationScore);
                Monetary reputationBasedQuoteSideAmount = BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(marketPriceService, market, sellersReputationScore)
                        .orElseThrow().round(0);
                model.setSellersReputationBasedQuoteSideAmount(reputationBasedQuoteSideAmount);
            }
            long sellersReputationScore = model.getSellersReputationScore();
            Monetary reputationBasedQuoteSideAmount = model.getSellersReputationBasedQuoteSideAmount().round(0);

            if (reputationBasedQuoteSideAmount.isLessThan(maxRangeValue)) {
                model.getIsAmountLimitInfoVisible().set(true);
                amountSelectionController.setRightMarkerQuoteSideValue(reputationBasedQuoteSideAmount);
                amountSelectionController.setMaxOrFixedQuoteSideAmount(reputationBasedQuoteSideAmount);
                String formattedAmount = AmountFormatter.formatAmountWithCode(reputationBasedQuoteSideAmount);

                if (sellersReputationScore <= MIN_REPUTATION_SCORE) {
                    if (reputationBasedQuoteSideAmount.isLessThan(minRangeValue)) {
                        // Min amount not covered by security from reputation score
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.minAmountNotCovered", sellersReputationScore));
                        model.getAmountLimitInfoAmount().set("");
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.tooHighMin.overlay.info", sellersReputationScore, formattedAmount) + "\n\n");
                    } else {
                        // Max amount not covered by security from reputation score
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.tooHighMax", sellersReputationScore));
                        model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfoAmount", formattedAmount));
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.tooHighMax.overlay.info", sellersReputationScore, formattedAmount) + "\n\n");
                    }
                } else {
                    if (reputationBasedQuoteSideAmount.isLessThan(minRangeValue)) {
                        // Min amount not covered by security from reputation score
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.minAmountNotCovered", sellersReputationScore));
                        model.getAmountLimitInfoAmount().set("");
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.tooHighMin.overlay.info", sellersReputationScore, formattedAmount) + "\n\n");
                    } else {
                        // Max amount not covered by security from reputation score
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.minAmountCovered", sellersReputationScore));
                        model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfoAmount", formattedAmount));
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.minAmountCovered.overlay.info", sellersReputationScore, formattedAmount) + "\n\n");
                    }
                }
            } else {
                model.getIsAmountLimitInfoVisible().set(false);
            }
        } else {
            // Seller case
            model.setLinkToWikiText(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.linkToWikiText"));
            String myProfileId = userIdentityService.getSelectedUserIdentity().getUserProfile().getId();
            long myReputationScore = reputationService.getReputationScore(myProfileId).getTotalScore();
            Monetary quoteSideAmount = amountSelectionController.getMaxOrFixedQuoteSideAmount().get();

            BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(marketPriceService, market, myReputationScore)
                    .ifPresent(myReputationBasedQuoteSideAmount -> {
                        myReputationBasedQuoteSideAmount = myReputationBasedQuoteSideAmount.round(0);
                        model.getIsAmountHyperLinkDisabled().set(myReputationBasedQuoteSideAmount.isGreaterThan(maxRangeValue));
                        amountSelectionController.setRightMarkerQuoteSideValue(myReputationBasedQuoteSideAmount);
                        String formattedAmount = AmountFormatter.formatAmountWithCode(myReputationBasedQuoteSideAmount);
                        model.getIsAmountLimitInfoVisible().set(true);
                        model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfoAmount", formattedAmount));
                        model.setAmountLimitInfoLink(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.link"));

                        Monetary amountWithoutReputationNeeded = BisqEasyTradeAmountLimits.usdToFiat(marketPriceService, market, MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION)
                                .orElseThrow().round(0);
                        String formattedAmountWithoutReputationNeeded = formatAmountWithCode(amountWithoutReputationNeeded);
                        if (myReputationBasedQuoteSideAmount.isLessThan(maxRangeValue)) {
                            if (myReputationScore <= MIN_REPUTATION_SCORE) {
                                model.getAmountLimitInfo().set(Res.get("bisqEasy.takeOffer.amount.seller.limitInfo.scoreTooLow", myReputationScore));
                                model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.takeOffer.amount.seller.limitInfo.overlay.info.scoreTooLow",
                                        formattedAmountWithoutReputationNeeded, myReputationScore) + "\n\n");
                            } else {
                                model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.sufficientScore", myReputationScore));
                                model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.wizard.limitInfo.overlay.info", myReputationScore, formattedAmount));
                            }
                        } else {
                            Monetary offersQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer).orElseThrow().round(0);
                            if (myReputationBasedQuoteSideAmount.isGreaterThan(offersQuoteSideMaxOrFixedAmount)) {
                                model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.sufficientScore", myReputationScore));
                                model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.info.sufficientScore", myReputationScore, formattedAmount) + "\n\n");
                            } else if (myReputationScore <= MIN_REPUTATION_SCORE) {
                                model.getAmountLimitInfoAmount().set(null);
                                model.getAmountLimitInfo().set(Res.get("bisqEasy.takeOffer.amount.seller.limitInfo.lowToleratedAmount", formattedAmountWithoutReputationNeeded));
                                model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.takeOffer.amount.seller.limitInfo.overlay.info.scoreTooLow",
                                        formattedAmountWithoutReputationNeeded, myReputationScore) + "\n\n");
                            } else {
                                model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.sufficientScore", myReputationScore));
                                model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.wizard.limitInfo.overlay.info", myReputationScore, formattedAmount));
                            }
                        }
                    });
            applyReputationBasedQuoteSideAmount();
        }
    }

    private void applyReputationBasedQuoteSideAmount() {
        amountSelectionController.setMaxOrFixedQuoteSideAmount(amountSelectionController.getRightMarkerQuoteSideValue().round(0));
    }

    private void maxOrFixedQuoteSideAmountChanged(Monetary value) {
        if (amountSelectionController.getRightMarkerQuoteSideValue() == null) {
            return;
        }
        model.getIsWarningIconVisible().set(value.round(0).getValue() > amountSelectionController.getRightMarkerQuoteSideValue().round(0).getValue());
    }
}
