package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.AmazonGiftCardAccount;
import bisq.account.accounts.fiat.AmazonGiftCardAccountPayload;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.amazon_gift_card.AmazonGiftCardAccountPayloadDto;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.common.FiatPaymentRailDto;
import bisq.api.dto.account.fiat.amazon_gift_card.CreateAmazonGiftCardAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.util.StringUtils;

public class AmazonGiftCardAccountDtoMapping {
    public static AmazonGiftCardAccount toBisq2Model(String accountName, CreateAmazonGiftCardAccountPayloadDto payloadDto) {
        String selectedCountryCode = payloadDto.selectedCountryCode();
        String selectedCurrencyCode = FiatCurrencyRepository.getCurrencyByCountryCode(selectedCountryCode).getCode();
        AmazonGiftCardAccountPayload payload = new AmazonGiftCardAccountPayload(
                StringUtils.createUid(),
                selectedCountryCode,
                selectedCurrencyCode,
                payloadDto.emailOrMobileNr()
        );
        return new AmazonGiftCardAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(AmazonGiftCardAccount account) {
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatAccountDtoMappingHelper.toChargebackRiskDto(account.getPaymentMethod().getPaymentRail());
        FiatCurrencyDto currency = FiatAccountDtoMappingHelper.toFiatCurrencyDto(account.getAccountPayload().getSelectedCurrencyCode());
        CountryDto country = FiatAccountDtoMappingHelper.toCountryDto(account.getCountry().getCode());

        return new PaymentAccountDto(
                account.getAccountName(),
                FiatPaymentRailDto.AMAZON_GIFT_CARD,
                new AmazonGiftCardAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        currency,
                        country,
                        account.getAccountPayload().getEmailOrMobileNr()
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
