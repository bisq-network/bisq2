package bisq.api.dto.mappings.account.crypto;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.crypto.OtherCryptoAssetAccount;
import bisq.account.accounts.crypto.OtherCryptoAssetAccountPayload;
import bisq.api.dto.account.AccountMetadataDto;
import bisq.api.dto.account.crypto.OtherCryptoAssetAccountDto;
import bisq.api.dto.account.crypto.OtherCryptoAssetAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountKeyMapping;
import bisq.api.dto.mappings.account.PaymentAccountMetadataDtoMapping;
import bisq.common.util.StringUtils;

public class OtherCryptoAssetAccountDtoMapping {
    public static OtherCryptoAssetAccount toBisq2Model(OtherCryptoAssetAccountDto dto) {
        OtherCryptoAssetAccountPayloadDto payloadDto = dto.accountPayload();
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
        PaymentAccountKeyMapping.KeyData keyData = PaymentAccountKeyMapping.createDefault();
        return new OtherCryptoAssetAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyData.keyPair(),
                keyData.keyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static OtherCryptoAssetAccountDto fromBisq2Model(OtherCryptoAssetAccount account) {
        AccountMetadataDto accountMetadata = PaymentAccountMetadataDtoMapping.mapAccountMetadata(account);

        OtherCryptoAssetAccountPayload payload = account.getAccountPayload();
        return new OtherCryptoAssetAccountDto(
                account.getAccountName(),
                new OtherCryptoAssetAccountPayloadDto(
                        payload.getCurrencyCode(),
                        payload.getPaymentMethod().getDisplayString(),
                        payload.getAddress(),
                        payload.isInstant(),
                        payload.getIsAutoConf(),
                        payload.getAutoConfNumConfirmations(),
                        payload.getAutoConfMaxTradeAmount(),
                        payload.getAutoConfExplorerUrls()
                ),
                accountMetadata.creationDate(),
                accountMetadata.tradeLimitInfo(),
                accountMetadata.tradeDuration()
        );
    }
}
