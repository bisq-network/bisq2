package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload;
import bisq.api.dto.account.AccountMetadataDto;
import bisq.api.dto.account.fiat.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.UserDefinedFiatAccountDto;
import bisq.api.dto.account.fiat.UserDefinedFiatAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountKeyMapping;
import bisq.api.dto.mappings.account.PaymentAccountMetadataDtoMapping;
import bisq.common.util.StringUtils;

public class UserDefinedFiatAccountDtoMapping {
    public static UserDefinedFiatAccount toBisq2Model(UserDefinedFiatAccountDto dto) {
        UserDefinedFiatAccountPayloadDto payloadDto = dto.accountPayload();
        UserDefinedFiatAccountPayload payload = new UserDefinedFiatAccountPayload(
                StringUtils.createUid(),
                payloadDto.accountData()
        );
        PaymentAccountKeyMapping.KeyData keyData = PaymentAccountKeyMapping.createDefault();
        return new UserDefinedFiatAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyData.keyPair(),
                keyData.keyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static UserDefinedFiatAccountDto fromBisq2Model(UserDefinedFiatAccount account) {
        AccountMetadataDto accountMetadata = PaymentAccountMetadataDtoMapping.mapAccountMetadata(account);
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatPaymentMethodChargebackRiskDto.valueOf(account.getPaymentMethod().getPaymentRail().getChargebackRisk().name());

        String currency = FiatAccountPayloadCurrencyMapping.toDisplayString(account.getAccountPayload());

        return new UserDefinedFiatAccountDto(
                account.getAccountName(),
                new UserDefinedFiatAccountPayloadDto(
                        chargebackRisk,
                        currency,
                        account.getPaymentMethod().getShortDisplayString(),
                        null,
                        account.getAccountPayload().getAccountData()
                ),
                accountMetadata.creationDate(),
                accountMetadata.tradeLimitInfo(),
                accountMetadata.tradeDuration()
        );
    }
}
