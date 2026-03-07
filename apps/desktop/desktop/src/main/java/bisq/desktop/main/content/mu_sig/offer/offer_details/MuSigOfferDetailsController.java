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

package bisq.desktop.main.content.mu_sig.offer.offer_details;

import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecFormatter;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PriceFormatter;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsHelper.createSecurityDepositInfo;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsHelper.formatDepositAmountAsBTC;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsHelper.formatFixedOfferAmounts;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsHelper.formatRangeOfferAmounts;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsHelper.getOfferAmounts;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsHelper.getPriceDetails;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.Amount;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FixAtMarketPriceDetails;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FixPriceDetails;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FixedAmount;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FixedOfferAmounts;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FloatPriceDetails;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FormattedFixedOfferAmounts;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.FormattedRangeOfferAmounts;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.MarketPriceDetails;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.OfferAmounts;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.RangeAmount;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.RangeOfferAmounts;
import static bisq.desktop.main.content.mu_sig.offer.offer_details.MuSigOfferDetailsRecords.SecurityDepositInfo;

@Slf4j
public class MuSigOfferDetailsController implements InitWithDataController<MuSigOfferDetailsController.InitData> {

    @Getter(AccessLevel.PROTECTED)
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final MuSigOffer muSigOffer;

        public InitData(MuSigOffer muSigOffer) {
            this.muSigOffer = muSigOffer;
        }
    }

    @Getter
    private final MuSigOfferDetailsView view;
    private final MuSigOfferDetailsModel model;
    private final MarketPriceService marketPriceService;

    public MuSigOfferDetailsController(ServiceProvider serviceProvider) {
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        model = new MuSigOfferDetailsModel();
        view = new MuSigOfferDetailsView(model, this);
    }

    @Override
    public void initWithData(InitData data) {
        MuSigOffer muSigOffer = data.getMuSigOffer();

        // Use getDisplayDirection() for UI labels in altcoin markets where base currency is not Bitcoin
        boolean isBuyOffer = muSigOffer.getDisplayDirection().isBuy();
        Market market = muSigOffer.getMarket();
        String quoteCurrencyCode = market.getQuoteCurrencyCode();
        String baseCurrencyCode = market.getBaseCurrencyCode();

        model.setDirection(isBuyOffer ?
                Res.get("muSig.offer.details.direction.buy") :
                Res.get("muSig.offer.details.direction.sell"));

        model.setQuoteSideCurrencyCode(quoteCurrencyCode);
        model.setBaseSideCurrencyCode(baseCurrencyCode);

        Optional<OfferAmounts> offerAmounts = getOfferAmounts(marketPriceService, muSigOffer);
        if (offerAmounts.isEmpty()) {
            log.warn("OfferAmounts could not be calculated for offerId={}", muSigOffer.getId());
            model.setBaseSideAmount(Res.get("data.na"));
            model.setQuoteSideAmount(Res.get("data.na"));
        } else {
            switch (offerAmounts.get()) {
                case RangeOfferAmounts rangeOfferAmounts -> {
                    FormattedRangeOfferAmounts formattedAmounts = formatRangeOfferAmounts(rangeOfferAmounts);
                    model.setBaseSideAmount(Res.get("muSig.offer.details.baseSideRangeAmount",
                            formattedAmounts.formattedMinBaseSideAmount(), formattedAmounts.formattedMaxBaseSideAmount(), baseCurrencyCode));
                    model.setQuoteSideAmount(Res.get("muSig.offer.details.quoteSideRangeAmount",
                            formattedAmounts.formattedMinQuoteSideAmount(), formattedAmounts.formattedMaxQuoteSideAmount(), quoteCurrencyCode));
                }
                case FixedOfferAmounts fixedOfferAmounts -> {
                    FormattedFixedOfferAmounts formattedAmounts = formatFixedOfferAmounts(fixedOfferAmounts);
                    model.setBaseSideAmount(Res.get("muSig.offer.details.baseSideAmount",
                            formattedAmounts.formattedBaseSideAmount(), baseCurrencyCode));
                    model.setQuoteSideAmount(Res.get("muSig.offer.details.quoteSideAmount",
                            formattedAmounts.formattedQuoteSideAmount(), quoteCurrencyCode));
                }
                default -> throw new IllegalStateException("Unexpected amounts type: " + offerAmounts.get());
            }
        }

        PriceSpec priceSpec = muSigOffer.getPriceSpec();
        Optional<PriceQuote> priceQuote = PriceUtil.findQuote(marketPriceService, priceSpec, market);
        String formattedPrice = priceQuote.map(PriceFormatter::format).orElse("");
        model.setPrice(Res.get("muSig.offer.details.price", formattedPrice, market.getMarketCodes()));

        String priceDetails = getPriceDetails(marketPriceService, priceSpec, market).map(
                details ->
                        switch (details) {
                            case MarketPriceDetails() -> Res.get("muSig.offer.details.priceDetails.marketPrice");
                            case FixAtMarketPriceDetails d -> Res.get("muSig.offer.details.priceDetails.fix.atMarket",
                                    d.marketPriceAsString(), d.marketCodes());
                            case FloatPriceDetails d -> Res.get("muSig.offer.details.priceDetails.float",
                                    d.percentAsString(), d.aboveOrBelow(), d.marketPriceAsString(), d.marketCodes());
                            case FixPriceDetails d -> Res.get("muSig.offer.details.priceDetails.fix",
                                    d.percentAsString(), d.aboveOrBelow(), d.marketPriceAsString(), d.marketCodes());
                            default -> throw new IllegalStateException("Unexpected details type: " + details);
                        }
        ).orElse("");
        model.setPriceDetails(priceDetails);

        List<PaymentMethodSpec<?>> quoteSidePaymentMethodSpecs = muSigOffer.getQuoteSidePaymentMethodSpecs();
        model.setQuoteSidePaymentMethods(PaymentMethodSpecFormatter.fromPaymentMethodSpecs(quoteSidePaymentMethodSpecs));
        model.setQuoteSidePaymentMethodDescription(
                quoteSidePaymentMethodSpecs.size() == 1
                        ? Res.get("muSig.offer.details.quoteSidePaymentMethodDescription")
                        : Res.get("muSig.offer.details.quoteSidePaymentMethodDescriptions")
        );

        Optional<SecurityDepositInfo> securityDepositInfo = createSecurityDepositInfo(muSigOffer, offerAmounts);
        if (securityDepositInfo.isEmpty()) {
            log.warn("SecurityDepositInfo could not be created for offerId={}", muSigOffer.getId());
            model.setSecurityDeposit(Res.get("data.na"));
        } else {
            SecurityDepositInfo deposit = securityDepositInfo.get();
            Optional<Amount> amount = deposit.amount();
            if (amount.isEmpty()) {
                log.warn("SecurityDepositInfo.amount could not be created for offerId={}", muSigOffer.getId());
                model.setSecurityDeposit(Res.get("muSig.offer.details.securityDeposit.naAmount", deposit.percentText(), Res.get("data.na")));
            } else {
                switch (amount.get()) {
                    case FixedAmount a ->
                            model.setSecurityDeposit(Res.get("muSig.offer.details.securityDeposit.fixedAmount",
                                    deposit.percentText(), formatDepositAmountAsBTC(a.amount(), false), a.amount().getCode()));
                    case RangeAmount a ->
                            model.setSecurityDeposit(Res.get("muSig.offer.details.securityDeposit.rangeAmount",
                                    deposit.percentText(),
                                    formatDepositAmountAsBTC(a.minAmount(), false),
                                    formatDepositAmountAsBTC(a.maxAmount(), false),
                                    a.maxAmount().getCode()));
                    default -> throw new IllegalStateException("Unexpected amount type: " + amount.get());
                }
            }
        }

        // TODO: dummy value for now until we have real fee calculation implemented
        model.setFee(Res.get("muSig.offer.details.fee"));

        model.setId(muSigOffer.getId());
        model.setDate(DateFormatter.formatDateTime(muSigOffer.getDate()));

        Optional<String> tradeTerms = OfferOptionUtil.findMakersTradeTerms(muSigOffer.getOfferOptions());
        model.setMakersTradeTermsVisible(tradeTerms.isPresent());
        tradeTerms.ifPresent(model::setMakersTradeTerms);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onClose() {
        OverlayController.hide();
    }
}
