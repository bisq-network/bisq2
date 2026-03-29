package bisq.api.dto.mappings.account.crypto;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.MoneroAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.crypto.MoneroAccountDto;
import bisq.api.dto.account.crypto.MoneroAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class MoneroAccountDtoMapping {
    public static MoneroAccount toBisq2Model(MoneroAccountDto dto) {
        MoneroAccountPayloadDto payloadDto = dto.accountPayload();
        MoneroAccountPayload payload = new MoneroAccountPayload(
                StringUtils.createUid(),
                payloadDto.address(),
                payloadDto.isInstant(),
                payloadDto.isAutoConf(),
                payloadDto.autoConfNumConfirmations(),
                payloadDto.autoConfMaxTradeAmount(),
                payloadDto.autoConfExplorerUrls(),
                payloadDto.useSubAddresses(),
                payloadDto.mainAddress(),
                payloadDto.privateViewKey(),
                payloadDto.subAddress(),
                payloadDto.accountIndex(),
                payloadDto.initialSubAddressIndex()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new MoneroAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static MoneroAccountDto fromBisq2Model(MoneroAccount account) {
        MoneroAccountPayload payload = account.getAccountPayload();
        return new MoneroAccountDto(
                account.getAccountName(),
                new MoneroAccountPayloadDto(
                        payload.getCurrencyCode(),
                        payload.getAddress(),
                        payload.isInstant(),
                        payload.getIsAutoConf(),
                        payload.getAutoConfNumConfirmations(),
                        payload.getAutoConfMaxTradeAmount(),
                        payload.getAutoConfExplorerUrls(),
                        payload.isUseSubAddresses(),
                        payload.getMainAddress(),
                        payload.getPrivateViewKey(),
                        payload.getSubAddress(),
                        payload.getAccountIndex(),
                        payload.getInitialSubAddressIndex()
                ),
                account.getCreationDate()
        );
    }
}
