package bisq.api.dto.mappings.account.crypto;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.crypto.OtherCryptoAssetAccount;
import bisq.account.accounts.crypto.OtherCryptoAssetAccountPayload;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.crypto.other_crypto_asset.OtherCryptoAssetAccountPayloadDto;
import bisq.api.dto.account.crypto.common.CryptoPaymentRailDto;
import bisq.api.dto.account.crypto.other_crypto_asset.CreateOtherCryptoAssetAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.asset.CryptoAssetRepository;
import bisq.common.util.StringUtils;

public class OtherCryptoAssetAccountDtoMapping {
    public static OtherCryptoAssetAccount toBisq2Model(String accountName, CreateOtherCryptoAssetAccountPayloadDto payloadDto) {
        OtherCryptoAssetAccountPayload payload = new OtherCryptoAssetAccountPayload(
                StringUtils.createUid(),
                payloadDto.currencyCode(),
                payloadDto.address(),
                payloadDto.isInstant(),
                payloadDto.isAutoConf(),
                payloadDto.autoConfNumConfirmations(),
                payloadDto.autoConfMaxTradeAmount(),
                payloadDto.autoConfExplorerUrls()
        );
        return new OtherCryptoAssetAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(OtherCryptoAssetAccount account) {

        OtherCryptoAssetAccountPayload payload = account.getAccountPayload();
        return new PaymentAccountDto(
                account.getAccountName(),
                CryptoPaymentRailDto.OTHER_CRYPTO_ASSET,
                new OtherCryptoAssetAccountPayloadDto(
                        payload.getCurrencyCode(),
                        payload.getPaymentMethod().getName(),
                        payload.getAddress(),
                        payload.isInstant(),
                        payload.getIsAutoConf(),
                        payload.getAutoConfNumConfirmations(),
                        payload.getAutoConfMaxTradeAmount(),
                        payload.getAutoConfExplorerUrls(),
                        CryptoAssetRepository.isAutoConfSupported(payload.getCurrencyCode())
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
