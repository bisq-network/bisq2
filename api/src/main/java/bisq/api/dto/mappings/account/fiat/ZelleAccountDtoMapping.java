package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.ZelleAccount;
import bisq.account.accounts.fiat.ZelleAccountPayload;
import bisq.api.dto.account.AccountMetadataDto;
import bisq.api.dto.account.fiat.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.ZelleAccountDto;
import bisq.api.dto.account.fiat.ZelleAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountKeyMapping;
import bisq.api.dto.mappings.account.PaymentAccountMetadataDtoMapping;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;

public class ZelleAccountDtoMapping {
    public static ZelleAccount toBisq2Model(ZelleAccountDto dto) {
        ZelleAccountPayloadDto payloadDto = dto.accountPayload();
        ZelleAccountPayload payload = new ZelleAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.emailOrMobileNr()
        );
        PaymentAccountKeyMapping.KeyData keyData = PaymentAccountKeyMapping.createDefault();
        return new ZelleAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyData.keyPair(),
                keyData.keyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static ZelleAccountDto fromBisq2Model(ZelleAccount account) {
        AccountMetadataDto accountMetadata = PaymentAccountMetadataDtoMapping.mapAccountMetadata(account);
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatPaymentMethodChargebackRiskDto.valueOf(account.getPaymentMethod().getPaymentRail().getChargebackRisk().name());

        String currency = FiatAccountPayloadCurrencyMapping.toDisplayString(account.getAccountPayload());

        return new ZelleAccountDto(
                account.getAccountName(),
                new ZelleAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        currency,
                        CountryRepository.getNameByCode(account.getCountry().getCode()),
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getEmailOrMobileNr()
                ),
                accountMetadata.creationDate(),
                accountMetadata.tradeLimitInfo(),
                accountMetadata.tradeDuration()
        );
    }
}
