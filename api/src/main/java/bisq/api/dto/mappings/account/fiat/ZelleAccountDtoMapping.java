package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.ZelleAccount;
import bisq.account.accounts.fiat.ZelleAccountPayload;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.common.FiatPaymentRailDto;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.zelle.ZelleAccountPayloadDto;
import bisq.api.dto.account.fiat.zelle.CreateZelleAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;

public class ZelleAccountDtoMapping {
    public static ZelleAccount toBisq2Model(String accountName, CreateZelleAccountPayloadDto payloadDto) {
        ZelleAccountPayload payload = new ZelleAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.emailOrMobileNr()
        );
        return new ZelleAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(ZelleAccount account) {
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatAccountDtoMappingHelper.toChargebackRiskDto(account.getPaymentMethod().getPaymentRail());

        String currency = FiatAccountPayloadCurrencyMapping.toDisplayString(account.getAccountPayload());

        return new PaymentAccountDto(
                account.getAccountName(),
                FiatPaymentRailDto.ZELLE,
                new ZelleAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        currency,
                        CountryRepository.getNameByCode(account.getCountry().getCode()),
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getEmailOrMobileNr()
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
