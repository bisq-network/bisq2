package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.RevolutAccount;
import bisq.account.accounts.fiat.RevolutAccountPayload;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.common.FiatPaymentRailDto;
import bisq.api.dto.account.fiat.revolut.RevolutAccountPayloadDto;
import bisq.api.dto.account.fiat.revolut.CreateRevolutAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.util.StringUtils;

import java.util.List;

public class RevolutAccountDtoMapping {
    public static RevolutAccount toBisq2Model(String accountName, CreateRevolutAccountPayloadDto payloadDto) {
        RevolutAccountPayload payload = new RevolutAccountPayload(
                StringUtils.createUid(),
                payloadDto.userName(),
                payloadDto.selectedCurrencyCodes()
        );
        return new RevolutAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(RevolutAccount account) {
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatAccountDtoMappingHelper.toChargebackRiskDto(account.getPaymentMethod().getPaymentRail());
        List<FiatCurrencyDto> selectedCurrencies = FiatAccountDtoMappingHelper.toFiatCurrencyDtos(account.getAccountPayload().getSelectedCurrencyCodes());

        return new PaymentAccountDto(
                account.getAccountName(),
                FiatPaymentRailDto.REVOLUT,
                new RevolutAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        selectedCurrencies,
                        account.getAccountPayload().getUserName()
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
