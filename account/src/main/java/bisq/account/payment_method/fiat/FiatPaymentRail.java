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

// rename to NationalFiatPaymentRail/NationalCurrencyPaymentRail/TraditionalPaymentRail

/**
 * The payment rails for fiat payments.
 * Provide static data associated with the payment rail.
 */
public enum FiatPaymentRail implements PaymentRail {
    // US
    ZELLE(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    STRIKE(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    WISE_USD(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    ACH_TRANSFER(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    DOMESTIC_WIRE_TRANSFER(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    US_POSTAL_MONEY_ORDER(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    @Deprecated(since = "2.1.8")
    // Has high chargeback risk and was added accidentally to Bisq 2. Only kept for backward compatibility for Bisq Easy 2.1.7
    CASH_APP(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // EUR
    SEPA(FiatPaymentRailUtil.getAllSepaCountries(),
            FiatCurrencyRepository.getCurrencyByCode("EUR"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    SEPA_INSTANT(FiatPaymentRailUtil.getAllSepaCountries(),
            FiatCurrencyRepository.getCurrencyByCode("EUR"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Spain
    BIZUM(countryFromCode("ES"),
            FiatCurrencyRepository.getCurrencyByCode("EUR"),
            FiatPaymentMethodChargebackRisk.LOW),

    HAL_CASH(countryFromCode("ES"),
            FiatCurrencyRepository.getCurrencyByCode("EUR"),
            FiatPaymentMethodChargebackRisk.LOW),

    // Poland (Polish brand of Halcash)
    PIN_4(countryFromCode("PL"),
            FiatCurrencyRepository.getCurrencyByCode("PLN"),
            FiatPaymentMethodChargebackRisk.LOW),

    // Sweden
    SWISH(countryFromCode("SE"),
            FiatCurrencyRepository.getCurrencyByCode("SEK"),
            FiatPaymentMethodChargebackRisk.LOW),

    // UK
    FASTER_PAYMENTS(countryFromCode("GB"),
            FiatCurrencyRepository.getCurrencyByCode("GBP"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Canada
    INTERAC_E_TRANSFER(countryFromCode("CA"),
            FiatCurrencyRepository.getCurrencyByCode("CAD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Japan
    // Not added yet due lack of language support and low usage
    // JAPAN_BANK = new PaymentMethod(JAPAN_BANK_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),

    // Australia
    PAY_ID(countryFromCode("AU"),
            FiatCurrencyRepository.getCurrencyByCode("AUD"),
            FiatPaymentMethodChargebackRisk.LOW),

    // Argentina
    // MERCADO_PAGO = new PaymentMethod(MERCADO_PAGO_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

    // Brazil
    PIX(countryFromCode("BR"),
            FiatCurrencyRepository.getCurrencyByCode("BRL"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // China
    // Not added yet due lack of language support and low usage
    // ALI_PAY = new PaymentMethod(ALI_PAY_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),
    // WECHAT_PAY = new PaymentMethod(WECHAT_PAY_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),

    // Thailand
    PROMPT_PAY(countryFromCode("TH"),
            FiatCurrencyRepository.getCurrencyByCode("THB"),
            FiatPaymentMethodChargebackRisk.LOW),

    // Russia
    // SBP = new PaymentMethod(SBP_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

    //India
    UPI(countryFromCode("IN"),
            FiatCurrencyRepository.getCurrencyByCode("INR"),
            FiatPaymentMethodChargebackRisk.LOW),
    // NEFT = new PaymentMethod(NEFT_ID, DAY, Coin.parseCoin("0.02")),
    // RTGS = new PaymentMethod(RTGS_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    // IMPS = new PaymentMethod(IMPS_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    // PAYTM = new PaymentMethod(PAYTM_ID, DAY, Coin.parseCoin("0.05")),


    // Global
    // WESTERN_UNION = new PaymentMethod(WESTERN_UNION_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_MID_RISK), N
    // SPECIFIC_BANKS = new PaymentMethod(SPECIFIC_BANKS_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

    CUSTOM(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Currency derived from country, but can be overridden to any
    F2F(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.LOW),

    // Select currency, no country in bisq 1
    CASH_BY_MAIL(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Currency derived from country, but can be overridden to any
    CASH_DEPOSIT(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Currency derived from country, but can be overridden to any
    NATIONAL_BANK(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    SAME_BANK(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    //todo not impl accounts yet
    SWIFT(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MEDIUM),

    // Trans national

    // User can define supported currencies
    REVOLUT(FiatPaymentRailUtil.getRevolutCountries(),
            FiatPaymentRailUtil.getRevolutCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // User can define currencies for receiving funds
    WISE(FiatPaymentRailUtil.getWiseCountries(),
            FiatPaymentRailUtil.getWiseCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    UPHOLD(FiatPaymentRailUtil.getUpholdCountries(),
            FiatPaymentRailUtil.getUpholdCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Selected currency is the currency derived from country
    AMAZON_GIFT_CARD(FiatPaymentRailUtil.getAmazonGiftCardCountries(),
            FiatPaymentRailUtil.getAmazonGiftCardCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    MONEY_BEAM(FiatPaymentRailUtil.getAllSepaCountries(),
            FiatPaymentRailUtil.getMoneyBeamCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    MONEY_GRAM(FiatPaymentRailUtil.getMoneyGramCountries(),
            FiatPaymentRailUtil.getMoneyGramCurrencies(),
            FiatPaymentMethodChargebackRisk.MEDIUM);


    // PERFECT_MONEY = new PaymentMethod(PERFECT_MONEY_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),
    // ADVANCED_CASH = new PaymentMethod(ADVANCED_CASH_ID, DAY, DEFAULT_TRADE_LIMIT_VERY_LOW_RISK),
    // PAYSERA = new PaymentMethod(PAYSERA_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    // PAXUM = new PaymentMethod(PAXUM_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),


    // NEQUI = new PaymentMethod(NEQUI_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    // CAPITUAL = new PaymentMethod(CAPITUAL_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    // CELPAY = new PaymentMethod(CELPAY_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    // MONESE = new PaymentMethod(MONESE_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    // SATISPAY = new PaymentMethod(SATISPAY_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    // TIKKIE = new PaymentMethod(TIKKIE_ID, DAY, Coin.parseCoin("0.05")),
    // VERSE = new PaymentMethod(VERSE_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

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
            case ZELLE -> DAYS_4;
            case STRIKE -> DAYS_4;
            case WISE_USD -> DAYS_4;
            case ACH_TRANSFER -> DAYS_5;
            case US_POSTAL_MONEY_ORDER -> DAYS_4;
            case CASH_APP -> DAYS_4;
            case SEPA -> DAYS_6;
            case SEPA_INSTANT -> HOURS_24;
            case MONEY_BEAM -> HOURS_24;
            case BIZUM -> DAYS_4;
            case PIN_4 -> HOURS_24;
            case SWISH -> HOURS_24;
            case FASTER_PAYMENTS -> HOURS_24;
            case INTERAC_E_TRANSFER -> HOURS_24;
            case PAY_ID -> DAYS_4;
            case PIX -> HOURS_24;
            case PROMPT_PAY -> HOURS_24;
            case UPI -> DAYS_4;
            case CUSTOM -> DAYS_4;
            case F2F -> DAYS_4;
            case CASH_BY_MAIL -> DAYS_8;
            case CASH_DEPOSIT -> DAYS_4;
            case NATIONAL_BANK -> DAYS_4;
            case SAME_BANK -> HOURS_24;
            case SWIFT -> DAYS_4;
            case DOMESTIC_WIRE_TRANSFER -> DAYS_3;
            case REVOLUT -> HOURS_24;
            case WISE -> DAYS_4;
            case UPHOLD -> HOURS_24;
            case AMAZON_GIFT_CARD -> DAYS_4;
            case MONEY_GRAM -> DAYS_4;
            case HAL_CASH -> HOURS_24;
        };
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FiatPaymentRail.class);
    }
}
/**
 * Not added methods from Bisq 1:
 * - All deprecated ones
 * - Japan Bank Furikomi: No language support yet
 * - AliPay, WeChat: No language support yet, AliPay likely mostly test trades. Bitcoin trade in China is illegal
 * - Popmoney: Popmoney was discontinued on June 30, 2023
 * <p>
 * Those are not planned to get added for release:
 * - Western Union: high fees, low usage
 * - Perfect Money: low usage, mostly test trades?
 * - Transfers with specific banks: Low usage, complex UI
 * - SWIFT International Wire Transfer: Low usage, complex UI, high fees
 * <p>
 * All those had low usage but might get added on demand:
 * [Monese] => 7
 * [Advanced Cash] => 6
 * [Paysera] => 6
 * [Satispay] => 4
 * [Faster Payments System (SBP)] => 3
 * [Payment method] => 1
 * [MercadoPago] => 1
 * [India/NEFT] => 1
 * [Verse] => 1
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

    /*
Most important methods are added already. we can add more on demand/request later:
Here are the missing ones (not updated with maybe newly added ones in Bisq1)
SAME_BANK=Transfer with same bank
SPECIFIC_BANKS=Transfers with specific banks
MONEY_GRAM=MoneyGram
WESTERN_UNION=Western Union
JAPAN_BANK=Japan Bank Furikomi
UPHOLD=Uphold
MONEY_BEAM=MoneyBeam (N26)
POPMONEY=Popmoney
PERFECT_MONEY=Perfect Money
ALI_PAY=AliPay
WECHAT_PAY=WeChat Pay
SEPA_INSTANT=SEPA Instant Payments
SWISH=Swish
CHASE_QUICK_PAY=Chase QuickPay
HAL_CASH=HalCash
PROMPT_PAY=PromptPay
ADVANCED_CASH=Advanced Cash
WISE_USD=Wise-USD
PAYSERA=Paysera
PAXUM=Paxum
NEFT=India/NEFT
RTGS=India/RTGS
IMPS=India/IMPS
PAYTM=India/PayTM
NEQUI=Nequi
CAPITUAL=Capitual
CELPAY=CelPay
MONESE=Monese
SATISPAY=Satispay
TIKKIE=Tikkie
VERSE=Verse
SWIFT=SWIFT International Wire Transfer
DOMESTIC_WIRE_TRANSFER=Domestic Wire Transfer
CIPS=Cross-Border Interbank Payment System
*/