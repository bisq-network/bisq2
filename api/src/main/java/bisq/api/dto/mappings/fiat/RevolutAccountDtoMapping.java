package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.RevolutAccount;
import bisq.account.accounts.fiat.RevolutAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.RevolutAccountDto;
import bisq.api.dto.account.fiat.RevolutAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class RevolutAccountDtoMapping {
    public static RevolutAccount toBisq2Model(RevolutAccountDto dto) {
        RevolutAccountPayloadDto payloadDto = dto.accountPayload();
        RevolutAccountPayload payload = new RevolutAccountPayload(
                StringUtils.createUid(),
                payloadDto.userName(),
                payloadDto.selectedCurrencyCodes()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new RevolutAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static RevolutAccountDto fromBisq2Model(RevolutAccount account) {
        return new RevolutAccountDto(
                account.getAccountName(),
                new RevolutAccountPayloadDto(
                        account.getAccountPayload().getUserName(),
                        account.getAccountPayload().getSelectedCurrencyCodes()
                )
        );
    }
}
