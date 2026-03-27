/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 */

package bisq.api.dto.account.fiat;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "paymentRail",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AchTransferAccountDto.class, name = "ACH_TRANSFER"),
        @JsonSubTypes.Type(value = AdvancedCashAccountDto.class, name = "ADVANCED_CASH"),
        @JsonSubTypes.Type(value = AliPayAccountDto.class, name = "ALI_PAY"),
        @JsonSubTypes.Type(value = AmazonGiftCardAccountDto.class, name = "AMAZON_GIFT_CARD"),
        @JsonSubTypes.Type(value = BizumAccountDto.class, name = "BIZUM"),
        @JsonSubTypes.Type(value = CashByMailAccountDto.class, name = "CASH_BY_MAIL"),
        @JsonSubTypes.Type(value = CashDepositAccountDto.class, name = "CASH_DEPOSIT"),
        @JsonSubTypes.Type(value = UserDefinedFiatAccountDto.class, name = "CUSTOM"),
        @JsonSubTypes.Type(value = DomesticWireTransferAccountDto.class, name = "DOMESTIC_WIRE_TRANSFER"),
        @JsonSubTypes.Type(value = F2FAccountDto.class, name = "F2F"),
        @JsonSubTypes.Type(value = FasterPaymentsAccountDto.class, name = "FASTER_PAYMENTS"),
        @JsonSubTypes.Type(value = HalCashAccountDto.class, name = "HAL_CASH"),
        @JsonSubTypes.Type(value = ImpsAccountDto.class, name = "IMPS"),
        @JsonSubTypes.Type(value = InteracETransferAccountDto.class, name = "INTERAC_E_TRANSFER"),
        @JsonSubTypes.Type(value = MercadoPagoAccountDto.class, name = "MERCADO_PAGO"),
        @JsonSubTypes.Type(value = MoneseAccountDto.class, name = "MONESE"),
        @JsonSubTypes.Type(value = MoneyBeamAccountDto.class, name = "MONEY_BEAM"),
        @JsonSubTypes.Type(value = MoneyGramAccountDto.class, name = "MONEY_GRAM"),
        @JsonSubTypes.Type(value = NationalBankAccountDto.class, name = "NATIONAL_BANK"),
        @JsonSubTypes.Type(value = NeftAccountDto.class, name = "NEFT"),
        @JsonSubTypes.Type(value = PayseraAccountDto.class, name = "PAYSERA"),
        @JsonSubTypes.Type(value = PayIdAccountDto.class, name = "PAY_ID"),
        @JsonSubTypes.Type(value = PerfectMoneyAccountDto.class, name = "PERFECT_MONEY"),
        @JsonSubTypes.Type(value = Pin4AccountDto.class, name = "PIN_4"),
        @JsonSubTypes.Type(value = PixAccountDto.class, name = "PIX"),
        @JsonSubTypes.Type(value = PromptPayAccountDto.class, name = "PROMPT_PAY"),
        @JsonSubTypes.Type(value = RevolutAccountDto.class, name = "REVOLUT"),
        @JsonSubTypes.Type(value = SameBankAccountDto.class, name = "SAME_BANK"),
        @JsonSubTypes.Type(value = SatispayAccountDto.class, name = "SATISPAY"),
        @JsonSubTypes.Type(value = SbpAccountDto.class, name = "SBP"),
        @JsonSubTypes.Type(value = SepaAccountDto.class, name = "SEPA"),
        @JsonSubTypes.Type(value = SepaInstantAccountDto.class, name = "SEPA_INSTANT"),
        @JsonSubTypes.Type(value = StrikeAccountDto.class, name = "STRIKE"),
        @JsonSubTypes.Type(value = SwiftAccountDto.class, name = "SWIFT"),
        @JsonSubTypes.Type(value = SwishAccountDto.class, name = "SWISH"),
        @JsonSubTypes.Type(value = UpholdAccountDto.class, name = "UPHOLD"),
        @JsonSubTypes.Type(value = UpiAccountDto.class, name = "UPI"),
        @JsonSubTypes.Type(value = USPostalMoneyOrderAccountDto.class, name = "US_POSTAL_MONEY_ORDER"),
        @JsonSubTypes.Type(value = WeChatPayAccountDto.class, name = "WECHAT_PAY"),
        @JsonSubTypes.Type(value = WiseAccountDto.class, name = "WISE"),
        @JsonSubTypes.Type(value = WiseUsdAccountDto.class, name = "WISE_USD"),
        @JsonSubTypes.Type(value = ZelleAccountDto.class, name = "ZELLE")
})
public interface FiatAccountDto {
    String accountName();

    FiatPaymentRailDto paymentRail();

    FiatAccountPayloadDto accountPayload();
}
