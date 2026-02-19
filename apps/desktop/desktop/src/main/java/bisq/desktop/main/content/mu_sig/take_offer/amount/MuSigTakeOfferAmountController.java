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

package bisq.desktop.main.content.mu_sig.take_offer.amount;

import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.bisq_easy.components.amount_selection.AmountSelectionController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.PriceUtil;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class MuSigTakeOfferAmountController implements Controller {
    private final MuSigTakeOfferAmountModel model;
    @Getter
    private final MuSigTakeOfferAmountView view;
    private final AmountSelectionController amountSelectionController;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final ReputationService reputationService;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private Subscription baseSideAmountPin, quoteSideAmountPin;

    public MuSigTakeOfferAmountController(ServiceProvider serviceProvider,
                                          Consumer<Boolean> navigationButtonsVisibleHandler) {
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        model = new MuSigTakeOfferAmountModel();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        amountSelectionController = new AmountSelectionController(serviceProvider);
        view = new MuSigTakeOfferAmountView(model, this, amountSelectionController.getView().getRoot());
    }

    public void init(MuSigOffer muSigOffer) {
        model.setMuSigOffer(muSigOffer);

        Direction takersDirection = muSigOffer.getTakersDirection();
        model.setHeadline(takersDirection.isBuy()
                ? Res.get("bisqEasy.takeOffer.amount.headline.buyer")
                : Res.get("bisqEasy.takeOffer.amount.headline.seller"));
        amountSelectionController.setDirection(takersDirection);
        Market market = muSigOffer.getMarket();
        amountSelectionController.setMarket(market);

        PriceUtil.findQuote(marketPriceService, muSigOffer.getPriceSpec(), muSigOffer.getMarket())
                .ifPresent(amountSelectionController::setQuote);

        applyQuoteSideMinMaxRange();

        String btcAmount = takersDirection.isBuy()
                ? Res.get("bisqEasy.component.amount.baseSide.tooltip.buyer.btcAmount")
                : Res.get("bisqEasy.component.amount.baseSide.tooltip.seller.btcAmount");
        Optional<String> priceQuoteOptional = PriceUtil.findQuote(marketPriceService, model.getMuSigOffer())
                .map(priceQuote -> "\n" + Res.get("bisqEasy.component.amount.baseSide.tooltip.taker.offerPrice", PriceFormatter.formatWithCode(priceQuote)));
        priceQuoteOptional.ifPresent(priceQuote -> amountSelectionController.setTooltip(String.format("%s%s", btcAmount, priceQuote)));
    }

    public ReadOnlyObjectProperty<Monetary> getTakersQuoteSideAmount() {
        return model.getTakersQuoteSideAmount();
    }

    public ReadOnlyObjectProperty<Monetary> getTakersBaseSideAmount() {
        return model.getTakersBaseSideAmount();
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        amountSelectionController.setAllowInvertingBaseAndQuoteCurrencies(true);
        amountSelectionController.setBaseAsInputCurrency(true);
        applyQuoteSideMinMaxRange();
        baseSideAmountPin = EasyBind.subscribe(amountSelectionController.getMaxOrFixedBaseSideAmount(),
                amount -> {
                    model.getTakersBaseSideAmount().set(amount);
                });
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

        navigationButtonsVisibleHandler.accept(true);
        model.getIsWarningIconVisible().set(false);
        model.getIsAmountLimitInfoOverlayVisible().set(false);
        model.setSellersReputationBasedQuoteSideAmount(null);
    }

    void onSetReputationBasedAmount() {
        amountSelectionController.setMaxOrFixedQuoteSideAmount(amountSelectionController.getRightMarkerQuoteSideValue().round(0));
    }

    void onKeyPressedWhileShowingOverlay(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
        });
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseAmountLimitInfoOverlay);
    }

    void onShowAmountLimitInfoOverlay() {
        navigationButtonsVisibleHandler.accept(false);
        model.getIsAmountLimitInfoOverlayVisible().set(true);
    }

    void onCloseAmountLimitInfoOverlay() {
        navigationButtonsVisibleHandler.accept(true);
        model.getIsAmountLimitInfoOverlayVisible().set(false);
    }

    void onOpenWiki(String url) {
        Browser.open(url);
    }

    private void applyQuoteSideMinMaxRange() {
        MuSigOffer muSigOffer = model.getMuSigOffer();
        if (muSigOffer == null) {
            return;
        }
        Optional<Monetary> optionalQuoteSideMinAmount = OfferAmountUtil.findQuoteSideMinAmount(marketPriceService, muSigOffer);
        if (optionalQuoteSideMinAmount.isEmpty()) {
            return;
        }

        Market market = muSigOffer.getMarket();
        String myProfileId = userIdentityService.getSelectedUserIdentity().getUserProfile().getId();
        String makersUserProfileId = muSigOffer.getMakersUserProfileId();
        if (model.getSellersReputationBasedQuoteSideAmount() == null) {
            String sellersProfileId = muSigOffer.getDisplayDirection().isSell() ? makersUserProfileId : myProfileId;
            long sellersReputationScore = reputationService.getReputationScore(sellersProfileId).getTotalScore();
            model.setSellersReputationScore(sellersReputationScore);
            Monetary reputationBasedQuoteSideAmount = BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(marketPriceService, market, sellersReputationScore)
                    .orElseThrow()
                    .round(0);
            model.setSellersReputationBasedQuoteSideAmount(reputationBasedQuoteSideAmount);
        }
        long sellersReputationScore = model.getSellersReputationScore();
        Monetary reputationBasedQuoteSideAmount = model.getSellersReputationBasedQuoteSideAmount().round(0);
        Monetary offersQuoteSideMaxOrFixedAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, muSigOffer).orElseThrow().round(0);
        Monetary minRangeValue = optionalQuoteSideMinAmount.get().round(0);
        Monetary maxAmount = reputationBasedQuoteSideAmount.isLessThan(offersQuoteSideMaxOrFixedAmount)
                ? reputationBasedQuoteSideAmount
                : offersQuoteSideMaxOrFixedAmount;

        amountSelectionController.setMaxAllowedLimitation(offersQuoteSideMaxOrFixedAmount);
        amountSelectionController.setRightMarkerQuoteSideValue(maxAmount);
        amountSelectionController.setMinMaxRange(minRangeValue, maxAmount);

        boolean isBuyer = muSigOffer.getTakersDirection().isBuy();
        if (isBuyer) {
            // Buyer case
            model.setAmountLimitInfoLink(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.learnMore"));
            model.setLinkToWikiText(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.overlay.linkToWikiText"));

            if (reputationBasedQuoteSideAmount.isLessThan(offersQuoteSideMaxOrFixedAmount)) {
                // Max amount not covered by security from reputation score
                model.getIsAmountLimitInfoVisible().set(true);
                model.getAmountLimitInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.minAmountCovered", sellersReputationScore));
                String formattedAmount = AmountFormatter.formatQuoteAmountWithCode(reputationBasedQuoteSideAmount);
                model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfoAmount", formattedAmount));
                model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.takeOffer.amount.buyer.limitInfo.minAmountCovered.overlay.info", sellersReputationScore, formattedAmount) + "\n\n");
            } else {
                model.getIsAmountLimitInfoVisible().set(false);
            }
        } else {
            // Seller case
            model.setLinkToWikiText(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.linkToWikiText"));
            long myReputationScore = reputationService.getReputationScore(myProfileId).getTotalScore();
            BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(marketPriceService, market, myReputationScore)
                    .ifPresent(myReputationBasedQuoteSideAmount -> {
                        myReputationBasedQuoteSideAmount = myReputationBasedQuoteSideAmount.round(0);
                        model.getIsAmountHyperLinkDisabled().set(myReputationBasedQuoteSideAmount.isGreaterThan(offersQuoteSideMaxOrFixedAmount));
                        amountSelectionController.setRightMarkerQuoteSideValue(myReputationBasedQuoteSideAmount);
                        model.getIsAmountLimitInfoVisible().set(false);
                        String formattedAmount = AmountFormatter.formatQuoteAmountWithCode(myReputationBasedQuoteSideAmount);
                        model.getAmountLimitInfoAmount().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfoAmount", formattedAmount));
                        model.setAmountLimitInfoLink(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.link"));
                        if (myReputationBasedQuoteSideAmount.isLessThan(offersQuoteSideMaxOrFixedAmount)) {
                            model.getIsAmountLimitInfoVisible().set(true);
                            model.getAmountLimitInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.insufficientScore", myReputationScore));
                            model.getAmountLimitInfoOverlayInfo().set(Res.get("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay.info.insufficientScore", myReputationScore, formattedAmount) + "\n\n");
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
        model.getIsWarningIconVisible().set(value.round(0).equals(amountSelectionController.getRightMarkerQuoteSideValue().round(0)));
    }
}
