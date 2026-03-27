package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.AliPayAccount;
import bisq.account.accounts.fiat.AliPayAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.AliPayAccountDto;
import bisq.api.dto.account.fiat.AliPayAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class AliPayAccountDtoMapping {
    public static AliPayAccount toBisq2Model(AliPayAccountDto dto) {
        AliPayAccountPayloadDto payloadDto = dto.accountPayload();
        AliPayAccountPayload payload = new AliPayAccountPayload(
                StringUtils.createUid(),
                payloadDto.accountNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new AliPayAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static AliPayAccountDto fromBisq2Model(AliPayAccount account) {
        return new AliPayAccountDto(
                account.getAccountName(),
                new AliPayAccountPayloadDto(
                        account.getAccountPayload().getAccountNr()
                )
        );
    }
}
