package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.common.FiatPaymentRailDto;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.user_defined.UserDefinedFiatAccountPayloadDto;
import bisq.api.dto.account.fiat.user_defined.CreateUserDefinedFiatAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.util.StringUtils;

public class UserDefinedFiatAccountDtoMapping {
    public static UserDefinedFiatAccount toBisq2Model(String accountName, CreateUserDefinedFiatAccountPayloadDto payloadDto) {
        UserDefinedFiatAccountPayload payload = new UserDefinedFiatAccountPayload(
                StringUtils.createUid(),
                payloadDto.accountData()
        );
        return new UserDefinedFiatAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(UserDefinedFiatAccount account) {
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatPaymentMethodChargebackRiskDto.valueOf(account.getPaymentMethod().getPaymentRail().getChargebackRisk().name());

        return new PaymentAccountDto(
                account.getAccountName(),
                FiatPaymentRailDto.CUSTOM,
                new UserDefinedFiatAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        account.getAccountPayload().getAccountData()
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
