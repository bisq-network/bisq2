package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.AliPayAccount;
import bisq.account.accounts.fiat.AliPayAccountPayload;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.alipay.AliPayAccountPayloadDto;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.common.FiatPaymentRailDto;
import bisq.api.dto.account.fiat.alipay.CreateAliPayAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.util.StringUtils;

public class AliPayAccountDtoMapping {
    public static AliPayAccount toBisq2Model(String accountName, CreateAliPayAccountPayloadDto payloadDto) {
        AliPayAccountPayload payload = new AliPayAccountPayload(
                StringUtils.createUid(),
                payloadDto.accountNr()
        );
        return new AliPayAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(AliPayAccount account) {
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatAccountDtoMappingHelper.toChargebackRiskDto(account.getPaymentMethod().getPaymentRail());
        FiatCurrencyDto currency = FiatAccountDtoMappingHelper.toFiatCurrencyDto(account.getAccountPayload().getCurrencyCode());
        CountryDto country = FiatAccountDtoMappingHelper.toCountryDto(account.getCountry().getCode());

        return new PaymentAccountDto(
                account.getAccountName(),
                FiatPaymentRailDto.ALI_PAY,
                new AliPayAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        currency,
                        country,
                        account.getAccountPayload().getAccountNr()
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
