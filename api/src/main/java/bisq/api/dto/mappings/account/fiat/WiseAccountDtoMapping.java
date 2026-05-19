package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.WiseAccount;
import bisq.account.accounts.fiat.WiseAccountPayload;
import bisq.api.dto.account.fiat.FiatCurrencyDto;
import bisq.api.dto.account.fiat.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.FiatPaymentRailDto;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.WiseAccountPayloadDto;
import bisq.api.dto.account.fiat.create.CreateWiseAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.util.StringUtils;

import java.util.List;

public class WiseAccountDtoMapping {
    public static WiseAccount toBisq2Model(String accountName, CreateWiseAccountPayloadDto payloadDto) {
        WiseAccountPayload payload = new WiseAccountPayload(
                StringUtils.createUid(),
                payloadDto.selectedCurrencyCodes(),
                payloadDto.holderName(),
                payloadDto.email()
        );
        return new WiseAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(WiseAccount account) {
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatAccountDtoMappingHelper.toChargebackRiskDto(account.getPaymentMethod().getPaymentRail());
        List<FiatCurrencyDto> selectedCurrencies = FiatAccountDtoMappingHelper.toFiatCurrencyDtos(account.getAccountPayload().getSelectedCurrencyCodes());

        return new PaymentAccountDto(
                account.getAccountName(),
                FiatPaymentRailDto.WISE,
                new WiseAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        selectedCurrencies,
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getEmail()
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
