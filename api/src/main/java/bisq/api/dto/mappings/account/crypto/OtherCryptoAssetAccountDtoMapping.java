package bisq.api.dto.mappings.account.crypto;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.crypto.OtherCryptoAssetAccount;
import bisq.account.accounts.crypto.OtherCryptoAssetAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.crypto.OtherCryptoAssetAccountDto;
import bisq.api.dto.account.crypto.OtherCryptoAssetAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

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
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new OtherCryptoAssetAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static OtherCryptoAssetAccountDto fromBisq2Model(OtherCryptoAssetAccount account) {
        OtherCryptoAssetAccountPayload payload = account.getAccountPayload();
        return new OtherCryptoAssetAccountDto(
                account.getAccountName(),
                new OtherCryptoAssetAccountPayloadDto(
                        payload.getCurrencyCode(),
                        payload.getAddress(),
                        payload.isInstant(),
                        payload.getIsAutoConf(),
                        payload.getAutoConfNumConfirmations(),
                        payload.getAutoConfMaxTradeAmount(),
                        payload.getAutoConfExplorerUrls()
                ),
                account.getCreationDate()
        );
    }
}
