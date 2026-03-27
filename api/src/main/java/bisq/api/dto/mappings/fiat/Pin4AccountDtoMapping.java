package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.Pin4Account;
import bisq.account.accounts.fiat.Pin4AccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.Pin4AccountDto;
import bisq.api.dto.account.fiat.Pin4AccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class Pin4AccountDtoMapping {
    public static Pin4Account toBisq2Model(Pin4AccountDto dto) {
        Pin4AccountPayloadDto payloadDto = dto.accountPayload();
        Pin4AccountPayload payload = new Pin4AccountPayload(
                StringUtils.createUid(),
                payloadDto.mobileNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new Pin4Account(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static Pin4AccountDto fromBisq2Model(Pin4Account account) {
        return new Pin4AccountDto(
                account.getAccountName(),
                new Pin4AccountPayloadDto(
                        account.getAccountPayload().getMobileNr()
                )
        );
    }
}
