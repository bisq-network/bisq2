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

package bisq.account.payment_method;

import bisq.common.currency.FiatCurrency;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The payment rails for fiat payments.
 * Provide static data associated with the payment rail.
 */
public enum FiatPaymentRail implements NationalCurrencyPaymentRail {
    // US
    ZELLE(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    STRIKE(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    ACH_TRANSFER(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    US_POSTAL_MONEY_ORDER(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    //todo remove
    CASH_APP(countryFromCode("US"),
            FiatCurrencyRepository.getCurrencyByCode("USD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // US_POSTAL_MONEY_ORDER, POPMONEY

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
            FiatPaymentMethodChargebackRisk.VERY_LOW),

    // MONEY_BEAM

    // UK
    FASTER_PAYMENTS(countryFromCode("GB"),
            FiatCurrencyRepository.getCurrencyByCode("GBP"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Sweden
    // SWISH


    // Canada
    INTERAC_E_TRANSFER(countryFromCode("CA"),
            FiatCurrencyRepository.getCurrencyByCode("CAD"),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Japan
    // JAPAN_BANK = new PaymentMethod(JAPAN_BANK_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),

    // Australia
    PAY_ID(countryFromCode("AU"),
            FiatCurrencyRepository.getCurrencyByCode("AUD"),
            FiatPaymentMethodChargebackRisk.VERY_LOW),

    // Argentina
    // MERCADO_PAGO = new PaymentMethod(MERCADO_PAGO_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

    // Brazil
    PIX(countryFromCode("BR"),
            FiatCurrencyRepository.getCurrencyByCode("BRL")
            , FiatPaymentMethodChargebackRisk.MODERATE),

    // China
    // ALI_PAY = new PaymentMethod(ALI_PAY_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),
    // WECHAT_PAY = new PaymentMethod(WECHAT_PAY_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),

    // Thailand
    // PROMPT_PAY = new PaymentMethod(PROMPT_PAY_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),

    // Russia
    // SBP = new PaymentMethod(SBP_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

    //India
    UPI(countryFromCode("IN"),
            FiatCurrencyRepository.getCurrencyByCode("INR"),
            FiatPaymentMethodChargebackRisk.VERY_LOW),
    //            NEFT = new PaymentMethod(NEFT_ID, DAY, Coin.parseCoin("0.02")),
    //            RTGS = new PaymentMethod(RTGS_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            IMPS = new PaymentMethod(IMPS_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            PAYTM = new PaymentMethod(PAYTM_ID, DAY, Coin.parseCoin("0.05")),


    // Global
    //CASH_DEPOSIT = new PaymentMethod(CASH_DEPOSIT_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            CASH_BY_MAIL = new PaymentMethod(CASH_BY_MAIL_ID, 8 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            MONEY_GRAM = new PaymentMethod(MONEY_GRAM_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_MID_RISK),
    //            WESTERN_UNION = new PaymentMethod(WESTERN_UNION_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_MID_RISK),
    //            NATIONAL_BANK = new PaymentMethod(NATIONAL_BANK_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            SAME_BANK = new PaymentMethod(SAME_BANK_ID, 2 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            SPECIFIC_BANKS = new PaymentMethod(SPECIFIC_BANKS_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            HAL_CASH = new PaymentMethod(HAL_CASH_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),
    //            AMAZON_GIFT_CARD = new PaymentMethod(AMAZON_GIFT_CARD_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

    CUSTOM(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Currency derived from country, but can be overridden to any
    F2F(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.VERY_LOW),

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

    //todo
    SAME_BANK(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    SWIFT(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.LOW),

    //todo
    DOMESTIC_WIRE_TRANSFER(allCountries(),
            allCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Trans national

    // User can define supported currencies
    REVOLUT(FiatPaymentRailUtil.getRevolutCountries(),
            FiatPaymentRailUtil.getRevolutCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // User can define currencies for receiving funds
    WISE(FiatPaymentRailUtil.getWiseCountries(),
            FiatPaymentRailUtil.getWiseCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE),

    // Selected currency is the currency derived from country
    AMAZON_GIFT_CARD(FiatPaymentRailUtil.getAmazonGiftCardsCountries(),
            FiatPaymentRailUtil.getAmazonGiftCardsCurrencies(),
            FiatPaymentMethodChargebackRisk.MODERATE);


    //  UPHOLD = new PaymentMethod(UPHOLD_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            PERFECT_MONEY = new PaymentMethod(PERFECT_MONEY_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),
    //            ADVANCED_CASH = new PaymentMethod(ADVANCED_CASH_ID, DAY, DEFAULT_TRADE_LIMIT_VERY_LOW_RISK),
    //            TRANSFERWISE_USD = new PaymentMethod(TRANSFERWISE_USD_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            PAYSERA = new PaymentMethod(PAYSERA_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            PAXUM = new PaymentMethod(PAXUM_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),


    //            NEQUI = new PaymentMethod(NEQUI_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            CAPITUAL = new PaymentMethod(CAPITUAL_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            CELPAY = new PaymentMethod(CELPAY_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            MONESE = new PaymentMethod(MONESE_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            SATISPAY = new PaymentMethod(SATISPAY_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            TIKKIE = new PaymentMethod(TIKKIE_ID, DAY, Coin.parseCoin("0.05")),
    //            VERSE = new PaymentMethod(VERSE_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
    //            DOMESTIC_WIRE_TRANSFER = new PaymentMethod(DOMESTIC_WIRE_TRANSFER_ID, 3 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

    // Altcoins
    //  BLOCK_CHAINS = new PaymentMethod(BLOCK_CHAINS_ID, DAY, DEFAULT_TRADE_LIMIT_VERY_LOW_RISK),
    // Altcoins with 1 hour trade period
    //  BLOCK_CHAINS_INSTANT = new PaymentMethod(BLOCK_CHAINS_INSTANT_ID, TimeUnit.HOURS.toMillis(1), DEFAULT_TRADE_LIMIT_VERY_LOW_RISK),


    private static List<FiatCurrency> allCurrencies() {
        return FiatCurrencyRepository.getAllCurrencies();
    }

    @Getter
    @EqualsAndHashCode.Exclude
    private final List<Country> supportedCountries;
    @Getter
    @EqualsAndHashCode.Exclude
    private final List<? extends TradeCurrency> supportedCurrencies;
    @Getter
    @EqualsAndHashCode.Exclude
    private final List<String> supportedCurrencyCodes;
    @EqualsAndHashCode.Exclude
    private final Set<String> supportedCurrencyCodesAsSet;
    @Getter
    private final FiatPaymentMethodChargebackRisk chargebackRisk;

    FiatPaymentRail(Country supportedCountries,
                    List<? extends TradeCurrency> supportedCurrencies,
                    FiatPaymentMethodChargebackRisk chargebackRisk) {
        this(List.of(supportedCountries), supportedCurrencies, chargebackRisk);
    }

    FiatPaymentRail(List<Country> supportedCountries,
                    TradeCurrency supportedCurrencies,
                    FiatPaymentMethodChargebackRisk chargebackRisk) {
        this(supportedCountries, List.of(supportedCurrencies), chargebackRisk);
    }

    FiatPaymentRail(Country supportedCountries,
                    TradeCurrency supportedCurrencies,
                    FiatPaymentMethodChargebackRisk chargebackRisk) {
        this(List.of(supportedCountries), List.of(supportedCurrencies), chargebackRisk);
    }

    FiatPaymentRail(List<Country> supportedCountries,
                    List<? extends TradeCurrency> supportedCurrencies,
                    FiatPaymentMethodChargebackRisk chargebackRisk) {
        this.supportedCountries = supportedCountries;
        this.supportedCurrencies = supportedCurrencies;
        this.chargebackRisk = chargebackRisk;

        supportedCurrencyCodes = supportedCurrencies.stream().map(TradeCurrency::getCode).collect(Collectors.toList());
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
        return CountryRepository.getCountries();
    }

    @Override
    public String getTradeLimit() {
        //todo
        switch (getChargebackRisk()) {
            case VERY_LOW -> {
                return "10000 USD";
            }
            case LOW -> {
                return "5000 USD";
            }
            default -> {
                return "2500 USD";
            }
        }
    }

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
            case ACH_TRANSFER -> DAYS_5;
            case US_POSTAL_MONEY_ORDER -> DAYS_4;
            case CASH_APP -> DAYS_4;
            case SEPA -> DAYS_6;
            case SEPA_INSTANT -> HOURS_24;
            case BIZUM -> DAYS_4;
            case FASTER_PAYMENTS -> DAYS_4;
            case INTERAC_E_TRANSFER -> DAYS_4;
            case PAY_ID -> DAYS_4;
            case PIX -> DAYS_4;
            case UPI -> DAYS_4;
            case CUSTOM -> DAYS_4;
            case F2F -> DAYS_4;
            case CASH_BY_MAIL -> DAYS_8;
            case CASH_DEPOSIT -> DAYS_4;
            case NATIONAL_BANK -> DAYS_4;
            case SAME_BANK -> DAYS_4;
            case SWIFT -> DAYS_4;
            case DOMESTIC_WIRE_TRANSFER -> DAYS_4;
            case REVOLUT -> HOURS_24;
            case WISE -> DAYS_4;
            case AMAZON_GIFT_CARD -> DAYS_4;
            default -> HOURS_24;
        };
    }
}

/*
    [SEPA] => 66417
    [Zelle (ClearXchange)] => 62403
    [Altcoins] => 43308
    [Revolut] => 37752
    [Pix] => 18478
    [Faster Payment System (UK)] => 18269
    [Altcoins Instant] => 16097
    [National bank transfer] => 14963
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