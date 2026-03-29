/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 */

package bisq.api.dto.account.fiat;

import bisq.api.dto.account.PaymentRailDto;

public enum FiatPaymentRailDto implements PaymentRailDto {
    ACH_TRANSFER,
    ADVANCED_CASH,
    ALI_PAY,
    AMAZON_GIFT_CARD,
    BIZUM,
    CASH_BY_MAIL,
    CASH_DEPOSIT,
    CUSTOM,
    DOMESTIC_WIRE_TRANSFER,
    F2F,
    FASTER_PAYMENTS,
    HAL_CASH,
    IMPS,
    INTERAC_E_TRANSFER,
    MERCADO_PAGO,
    MONESE,
    MONEY_BEAM,
    MONEY_GRAM,
    NATIONAL_BANK,
    NEFT,
    PAY_ID,
    PAYSERA,
    PERFECT_MONEY,
    PIN_4,
    PIX,
    PROMPT_PAY,
    REVOLUT,
    SAME_BANK,
    SATISPAY,
    SBP,
    SEPA,
    SEPA_INSTANT,
    STRIKE,
    SWIFT,
    SWISH,
    UPHOLD,
    UPI,
    US_POSTAL_MONEY_ORDER,
    WECHAT_PAY,
    WISE,
    WISE_USD,
    ZELLE
}
