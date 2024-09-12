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
import bisq.desktop.main.content.bisq_easy.components.AmountComponent;
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

    public void init(BisqEasyOffer bisqEasyOffer, Optional<Monetary> optionalReputationBasedQuoteSideAmount) {
        model.setBisqEasyOffer(bisqEasyOffer);

        Direction takersDirection = bisqEasyOffer.getTakersDirection();
        model.setHeadline(takersDirection.isBuy() ? Res.get("bisqEasy.takeOffer.amount.headline.buyer") : Res.get("bisqEasy.takeOffer.amount.headline.seller"));
        amountComponent.setDirection(takersDirection);
        Market market = bisqEasyOffer.getMarket();
        amountComponent.setMarket(market);

        PriceUtil.findQuote(marketPriceService, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket())
                .ifPresent(amountComponent::setQuote);

        Optional<Monetary> OptionalQuoteSideMinAmount = OfferAmountUtil.findQuoteSideMinAmount(marketPriceService, bisqEasyOffer);
        if (OptionalQuoteSideMinAmount.isPresent()) {
            Monetary quoteSideMinAmount = OptionalQuoteSideMinAmount.get().round(0);
            Monetary offersQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer).orElseThrow().round(0);
            String formattedMinAmount = AmountFormatter.formatAmount(quoteSideMinAmount);
            Monetary maxAmount;
            if (optionalReputationBasedQuoteSideAmount.isPresent()) {
                // Range amounts seller case with provided reputationBasedQuoteSideAmount
                Monetary reputationBasedQuoteSideAmount = optionalReputationBasedQuoteSideAmount.get();
                maxAmount = reputationBasedQuoteSideAmount.isGreaterThan(offersQuoteSideMaxOrFixedAmount)
                        ? offersQuoteSideMaxOrFixedAmount
                        : reputationBasedQuoteSideAmount;
                amountComponent.setReputationBasedQuoteSideAmount(maxAmount);
                applyQuoteSideMinMaxRange(quoteSideMinAmount, maxAmount);

                long sellersScore = reputationService.getReputationScore(userIdentityService.getSelectedUserIdentity().getUserProfile()).getTotalScore();
                amountComponent.setDescription(Res.get("bisqEasy.takeOffer.amount.description.limitedByTakersReputation",
                        sellersScore,
                        formattedMinAmount,
                        AmountFormatter.formatAmountWithCode(maxAmount)));
            } else {
                // Range amounts buyer case
                maxAmount = offersQuoteSideMaxOrFixedAmount;
                applyQuoteSideMinMaxRange(quoteSideMinAmount, maxAmount);
                amountComponent.setDescription(Res.get("bisqEasy.takeOffer.amount.description",
                        formattedMinAmount,
                        AmountFormatter.formatAmountWithCode(maxAmount)));
            }
            String btcAmount = takersDirection.isBuy()
                    ? Res.get("bisqEasy.component.amount.baseSide.tooltip.buyer.btcAmount")
                    : Res.get("bisqEasy.component.amount.baseSide.tooltip.seller.btcAmount");
            Optional<String> priceQuoteOptional = PriceUtil.findQuote(marketPriceService, model.getBisqEasyOffer())
                    .map(priceQuote -> "\n" + Res.get("bisqEasy.component.amount.baseSide.tooltip.taker.offerPrice", PriceFormatter.formatWithCode(priceQuote)));
            priceQuoteOptional.ifPresent(priceQuote -> amountComponent.setTooltip(String.format("%s%s", btcAmount, priceQuote)));
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
        view.getRoot().setOnKeyPressed(null);
        model.getIsWarningIconVisible().set(false);
        model.getIsAmountLimitInfoOverlayVisible().set(false);
        model.setSellersReputationBasedQuoteSideAmount(null);
    }

    void onSetReputationBasedAmount() {
        amountComponent.setQuoteSideAmount(amountComponent.getReputationBasedQuoteSideAmount().round(0));
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
        amountComponent.setMinMaxRange(minRangeValue, maxRangeValue);

        BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
        if (bisqEasyOffer.getTakersDirection().isBuy()) {
            model.setAmountLimitInfoLink(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.learnMore"));
            model.setLinkToWikiText(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.overlay.linkToWikiText"));
            model.getAmountLimitInfoAmount().set("");

            if (model.getSellersReputationBasedQuoteSideAmount() == null) {
                long sellersReputationScore = reputationService.getReputationScore(bisqEasyOffer.getMakersUserProfileId()).getTotalScore();
                model.setSellersReputationScore(sellersReputationScore);
                Monetary reputationBasedQuoteSideAmount = BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(marketPriceService, bisqEasyOffer.getMarket(), sellersReputationScore)
                        .orElseThrow().round(0);
                model.setSellersReputationBasedQuoteSideAmount(reputationBasedQuoteSideAmount);
            }
            long sellersReputationScore = model.getSellersReputationScore();
            Monetary reputationBasedQuoteSideAmount = model.getSellersReputationBasedQuoteSideAmount();

            if (reputationBasedQuoteSideAmount.isLessThan(maxRangeValue)) {
                model.getIsAmountLimitInfoVisible().set(true);
                amountComponent.setReputationBasedQuoteSideAmount(reputationBasedQuoteSideAmount);
                amountComponent.setQuoteSideAmount(reputationBasedQuoteSideAmount);
                String formattedAmount = AmountFormatter.formatAmountWithCode(reputationBasedQuoteSideAmount);
                model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.overlay.info", sellersReputationScore, formattedAmount) + "\n\n");
                if (reputationBasedQuoteSideAmount.isLessThan(minRangeValue)) {
                    // Min amount not covered by security from reputation score
                    model.getAmountLimitInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.tooHighMin", sellersReputationScore));
                    model.getAmountLimitInfoAmount().set("");
                } else {
                    // Max amount not covered by security from reputation score
                    model.getAmountLimitInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.tooHighMax", sellersReputationScore));
                    model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfoAmount", formattedAmount));
                }
            } else {
                model.getIsAmountLimitInfoVisible().set(false);
            }
        } else {
            model.setLinkToWikiText(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.linkToWikiText"));
            String myProfileId = userIdentityService.getSelectedUserIdentity().getUserProfile().getId();
            long myReputationScore = reputationService.getReputationScore(myProfileId).getTotalScore();
            BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(marketPriceService, bisqEasyOffer.getMarket(), myReputationScore)
                    .ifPresent(myReputationBasedQuoteSideAmount -> {
                        model.getIsAmountHyperLinkDisabled().set(myReputationBasedQuoteSideAmount.isGreaterThan(maxRangeValue));
                        amountComponent.setReputationBasedQuoteSideAmount(myReputationBasedQuoteSideAmount);
                        String formattedAmount = AmountFormatter.formatAmountWithCode(myReputationBasedQuoteSideAmount);
                        model.getIsAmountLimitInfoVisible().set(true);
                        model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo", myReputationScore));
                        model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfoAmount", formattedAmount));
                        model.setAmountLimitInfoLink(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.link"));
                        model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.info", myReputationScore, formattedAmount));
                    });
            applyReputationBasedQuoteSideAmount();
        }
    }

    private void applyReputationBasedQuoteSideAmount() {
        amountComponent.setQuoteSideAmount(amountComponent.getReputationBasedQuoteSideAmount().round(0));
    }

    private void maxOrFixedQuoteSideAmountChanged(Monetary value) {
        if (amountComponent.getReputationBasedQuoteSideAmount() == null) {
            return;
        }
        model.getIsWarningIconVisible().set(value.round(0).getValue() > amountComponent.getReputationBasedQuoteSideAmount().round(0).getValue());
    }
}
