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

package bisq.account.payment_method.fiat;

import bisq.account.payment_method.PaymentRail;
import bisq.common.asset.Asset;
import bisq.common.asset.FiatCurrency;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The payment rails for fiat payments.
 * Provide static data associated with the payment rail.
 */
public enum FiatPaymentRail implements PaymentRail {
    ACH_TRANSFER(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    ADVANCED_CASH(allCountries(),
            FiatPaymentRailUtil.getAdvancedCashCurrencies(),
            FiatPaymentMethodChargebackRisk.VERY_LOW),

    ALI_PAY(countryFromCode("CN"),
            FiatCurrencyRepository.getCurrencyByCode("CNY"),
            FiatPaymentMethodChargebackRisk.LOW),

    AMAZON_GIFT_CARD(FiatPaymentRailUtil.getAmazonGiftCardCountries(),
            FiatPaymentRailUtil.getAmazonGiftCardCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    BIZUM(countryFromCode("ES"),
            FiatCurrencyRepository.getCurrencyByCode("EUR"),
            FiatPaymentMethodChargebackRisk.LOW),

    @Deprecated(since = "2.1.8")
    // Has high chargeback risk and was added accidentally to Bisq 2. Only kept for backward compatibility for Bisq Easy 2.1.7
    CASH_APP(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    CASH_BY_MAIL(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    CASH_DEPOSIT(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Not added as low usage and expensive fees
    // WESTERN_UNION = new PaymentMethod(WESTERN_UNION_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_MID_RISK),
    // Not added as complex and low usage
    // SPECIFIC_BANKS = new PaymentMethod(SPECIFIC_BANKS_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    CUSTOM(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    DOMESTIC_WIRE_TRANSFER(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    F2F(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.LOW),

    FASTER_PAYMENTS(countryFromCode("GB"),
            FiatCurrencyRepository.getCurrencyByCode("GBP"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    HAL_CASH(countryFromCode("ES"),
            FiatCurrencyRepository.getCurrencyByCode("EUR"),
            FiatPaymentMethodChargebackRisk.LOW),

    IMPS(countryFromCode("IN"),
            FiatCurrencyRepository.getCurrencyByCode("INR"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    INTERAC_E_TRANSFER(countryFromCode("CA"),
            FiatCurrencyRepository.getCurrencyByCode("CAD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Japan
    // Not added due lack of language support and low usage
    // JAPAN_BANK = new PaymentMethod(JAPAN_BANK_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),

    MERCADO_PAGO(countryFromCode("AR"),
            FiatCurrencyRepository.getCurrencyByCode("ARS"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    MONESE(allCountries(),
            FiatPaymentRailUtil.getMoneseCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    MONEY_BEAM(FiatPaymentRailUtil.getAllSepaCountries(),
            FiatPaymentRailUtil.getMoneyBeamCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    MONEY_GRAM(FiatPaymentRailUtil.getMoneyGramCountries(),
            FiatPaymentRailUtil.getMoneyGramCurrencies(),
            FiatPaymentMethodChargebackRisk.MEDIUM),

    NATIONAL_BANK(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    NEFT(countryFromCode("IN"),
            FiatCurrencyRepository.getCurrencyByCode("INR"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    PAY_ID(countryFromCode("AU"),
            FiatCurrencyRepository.getCurrencyByCode("AUD"),
            FiatPaymentMethodChargebackRisk.LOW),

    PAYSERA(allCountries(),
            FiatPaymentRailUtil.getPayseraCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    PERFECT_MONEY(allCountries(),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.LOW),

    PIN_4(countryFromCode("PL"),
            FiatCurrencyRepository.getCurrencyByCode("PLN"),
            FiatPaymentMethodChargebackRisk.LOW),

    PIX(countryFromCode("BR"),
            FiatCurrencyRepository.getCurrencyByCode("BRL"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    PROMPT_PAY(countryFromCode("TH"),
            FiatCurrencyRepository.getCurrencyByCode("THB"),
            FiatPaymentMethodChargebackRisk.LOW),

    REVOLUT(FiatPaymentRailUtil.getRevolutCountries(),
            FiatPaymentRailUtil.getRevolutCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    SAME_BANK(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    SATISPAY(countryFromCode("IT"),
            FiatCurrencyRepository.getCurrencyByCode("EUR"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    SBP(countryFromCode("RU"),
            FiatCurrencyRepository.getCurrencyByCode("RUB"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    SEPA(FiatPaymentRailUtil.getAllSepaCountries(),
            FiatCurrencyRepository.getCurrencyByCode("EUR"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    SEPA_INSTANT(FiatPaymentRailUtil.getAllSepaCountries(),
            FiatCurrencyRepository.getCurrencyByCode("EUR"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    STRIKE(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    //todo not impl accounts yet
    SWIFT(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MEDIUM),

    SWISH(countryFromCode("SE"),
            FiatCurrencyRepository.getCurrencyByCode("SEK"),
            FiatPaymentMethodChargebackRisk.LOW),

    UPHOLD(FiatPaymentRailUtil.getUpholdCountries(),
            FiatPaymentRailUtil.getUpholdCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    UPI(countryFromCode("IN"),
            FiatCurrencyRepository.getCurrencyByCode("INR"),
            FiatPaymentMethodChargebackRisk.LOW),

    US_POSTAL_MONEY_ORDER(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    VERSE(allCountries(),
            FiatPaymentRailUtil.getVerseCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    WECHAT_PAY(countryFromCode("CN"),
            FiatCurrencyRepository.getCurrencyByCode("CNY"),
            FiatPaymentMethodChargebackRisk.LOW),

    WISE(FiatPaymentRailUtil.getWiseCountries(),
            FiatPaymentRailUtil.getWiseCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    WISE_USD(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    ZELLE(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE);

    private static List<FiatCurrency> allCurrencies() {
        return FiatCurrencyRepository.getAllCurrencies();
    }

    @Getter
    @EqualsAndHashCode.Exclude
    private final List<Country> supportedCountries;
    @Getter
    @EqualsAndHashCode.Exclude
    private final List<? extends Asset> supportedCurrencies;
    @Getter
    @EqualsAndHashCode.Exclude
    private final List<String> supportedCurrencyCodes;
    @EqualsAndHashCode.Exclude
    private final Set<String> supportedCurrencyCodesAsSet;
    @Getter
    private final FiatPaymentMethodChargebackRisk chargebackRisk;

    FiatPaymentRail(Country supportedCountry,
                    List<? extends Asset> supportedCurrencies,
                    FiatPaymentMethodChargebackRisk chargebackRisk) {
        this(List.of(supportedCountry), supportedCurrencies, chargebackRisk);
    }

    FiatPaymentRail(List<Country> supportedCountries,
                    Asset supportedCurrency,
                    FiatPaymentMethodChargebackRisk chargebackRisk) {
        this(supportedCountries, List.of(supportedCurrency), chargebackRisk);
    }

    FiatPaymentRail(Country supportedCountry,
                    Asset supportedCurrency,
                    FiatPaymentMethodChargebackRisk chargebackRisk) {
        this(List.of(supportedCountry), List.of(supportedCurrency), chargebackRisk);
    }

    FiatPaymentRail(List<Country> supportedCountries,
                    List<? extends Asset> supportedCurrencies,
                    FiatPaymentMethodChargebackRisk chargebackRisk) {
        this.supportedCountries = supportedCountries;
        this.supportedCurrencies = supportedCurrencies;
        this.chargebackRisk = chargebackRisk;

        checkNotNull(supportedCurrencies, "supportedCurrencies must not be null");
        supportedCurrencyCodes = supportedCurrencies.stream()
                .map(asset -> {
                    if (asset == null) {
                        getLogger().error("When iterating supportedCurrencies, we got a null entry for the asset.");
                        return null;
                    }
                    String code = asset.getCode();
                    if (code == null) {
                        getLogger().error("When iterating supportedCurrencies, we got an asset with code set to null.");
                        getLogger().error("Asset with null code: {}", asset); // Log separately for case toString would cause an exception
                        return null;
                    }

                    return code;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        checkArgument(!supportedCurrencyCodes.isEmpty(), "supportedCurrencies must not be empty");
        supportedCurrencyCodesAsSet = new HashSet<>(supportedCurrencyCodes);
    }

    private static List<Country> countriesFromCodes(List<String> countryCodes) {
        return CountryRepository.getCountriesFromCodes(countryCodes);
    }

    private static List<FiatCurrency> currenciesFromCodes(List<String> currencyCodes) {
        return currencyCodes.stream()
                .map(FiatCurrencyRepository::getCurrencyByCountryCode)
                .collect(Collectors.toList());
    }

    private static Country countryFromCode(String countryCode) {
        return CountryRepository.getCountry(countryCode);
    }

    public boolean supportsCurrency(String currencyCode) {
        return supportedCurrencyCodesAsSet.contains(currencyCode);
    }

    private static List<Country> allCountries() {
        return CountryRepository.getAllCountries();
    }

    //todo use long instead of String
    @Override
    public String getTradeLimit() {
        //todo
        switch (getChargebackRisk()) {
            case LOW -> {
                return "10000 USD";
            }
            case MEDIUM -> {
                return "5000 USD";
            }
            default -> {
                return "2500 USD";
            }
        }
    }

    //todo add duration as enum field, use Duration not string (add method to get display string)
    private static final String HOURS_24 = Res.get("temporal.hour.*", 24);
    private static final String DAYS_2 = Res.get("temporal.day.*", 2);
    private static final String DAYS_3 = Res.get("temporal.day.*", 3);
    private static final String DAYS_4 = Res.get("temporal.day.*", 4);
    private static final String DAYS_5 = Res.get("temporal.day.*", 5);
    private static final String DAYS_6 = Res.get("temporal.day.*", 6);
    private static final String DAYS_7 = Res.get("temporal.day.*", 7);
    private static final String DAYS_8 = Res.get("temporal.day.*", 8);

    @Override
    public String getTradeDuration() {
        return switch (this) {
            case ACH_TRANSFER -> DAYS_5;
            case ADVANCED_CASH -> HOURS_24;
            case ALI_PAY -> HOURS_24;
            case AMAZON_GIFT_CARD -> DAYS_4;
            case BIZUM -> DAYS_4;
            case CASH_APP -> DAYS_4;
            case CASH_BY_MAIL -> DAYS_8;
            case CASH_DEPOSIT -> DAYS_4;
            case CUSTOM -> DAYS_4;
            case DOMESTIC_WIRE_TRANSFER -> DAYS_3;
            case F2F -> DAYS_4;
            case FASTER_PAYMENTS -> HOURS_24;
            case HAL_CASH -> HOURS_24;
            case IMPS -> HOURS_24;
            case INTERAC_E_TRANSFER -> HOURS_24;
            case MERCADO_PAGO -> HOURS_24;
            case MONESE -> HOURS_24;
            case MONEY_BEAM -> HOURS_24;
            case MONEY_GRAM -> DAYS_4;
            case NATIONAL_BANK -> DAYS_4;
            case NEFT -> HOURS_24;
            case PAY_ID -> DAYS_4;
            case PAYSERA -> HOURS_24;
            case PERFECT_MONEY -> HOURS_24;
            case PIN_4 -> HOURS_24;
            case PIX -> HOURS_24;
            case PROMPT_PAY -> HOURS_24;
            case REVOLUT -> HOURS_24;
            case SAME_BANK -> HOURS_24;
            case SATISPAY -> HOURS_24;
            case SBP -> HOURS_24;
            case SEPA -> DAYS_6;
            case SEPA_INSTANT -> HOURS_24;
            case STRIKE -> DAYS_4;
            case SWIFT -> DAYS_4;
            case SWISH -> HOURS_24;
            case UPHOLD -> HOURS_24;
            case UPI -> DAYS_4;
            case US_POSTAL_MONEY_ORDER -> DAYS_4;
            case VERSE -> HOURS_24;
            case WECHAT_PAY -> HOURS_24;
            case WISE -> DAYS_4;
            case WISE_USD -> DAYS_4;
            case ZELLE -> DAYS_4;
        };
    }

    public int getPopularityScore() {
        return FiatPaymentRailUtil.getPopularityScore().getOrDefault(this, 0);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FiatPaymentRail.class);
    }
}
/**
 * Not added methods from Bisq 1:
 * - All deprecated ones
 * - Japan Bank Furikomi: No language support yet
 * - Popmoney: Popmoney was discontinued on June 30, 2023
 * <p>
 * Those are not planned to get added for release:
 * - Western Union: high fees, low usage
 * - Transfers with specific banks: Low usage, complex UI
 * - SWIFT International Wire Transfer: Low usage, complex UI, high fees
 */

/*
    x[SEPA] => 66417
    x[Zelle (ClearXchange)] => 62403
    [Altcoins] => 43308
    x[Revolut] => 37752
    x[Pix] => 18478
    x[Faster Payment System (UK)] => 18269
    [Altcoins Instant] => 16097
    x[National bank transfer] => 14963
    [BSQ Swap] => 10063
    [SEPA Instant Payments] => 8991
    [Interac e-Transfer] => 8579
    [Strike] => 6427
    [Wise] => 5431
    [US Postal Money Order] => 2766
    [Cash By Mail] => 2253
    [Amazon eGift Card] => 1738
    [Australian PayID] => 1031
    [Wise-USD] => 867
    [Cash Deposit] => 798
    [ACH Transfer] => 782
    [MoneyBeam (N26)] => 442
    [Bizum] => 337
    [Transfer with same bank] => 298
    [Swish] => 291
    [Domestic Wire Transfer] => 239
    [Face to face (in person)] => 219
    [Cash App] => 178
    [Chase QuickPay] => 167
    [Uphold] => 143
    [Venmo] => 131
    [OKPay] => 85
    [Japan Bank Furikomi] => 63
    [AliPay] => 52
    [Popmoney] => 52
    [India/UPI] => 23
    [HalCash] => 22
    [Western Union] => 20
    [Transfers with specific banks] => 20
    [SWIFT International Wire Transfer] => 16
    [MoneyGram] => 12
    [WeChat Pay] => 10
    [Perfect Money] => 10
    [PromptPay] => 8
    [Monese] => 7
    [Advanced Cash] => 6
    [Paysera] => 6
    [Satispay] => 4
    [Faster Payments System (SBP)] => 3
    [Payment method] => 1
    [MercadoPago] => 1
    [India/NEFT] => 1
    [Verse] => 1
 */