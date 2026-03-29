package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.ZelleAccount;
import bisq.account.accounts.fiat.ZelleAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.ZelleAccountDto;
import bisq.api.dto.account.fiat.ZelleAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class ZelleAccountDtoMapping {
    public static ZelleAccount toBisq2Model(ZelleAccountDto dto) {
        ZelleAccountPayloadDto payloadDto = dto.accountPayload();
        ZelleAccountPayload payload = new ZelleAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.emailOrMobileNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new ZelleAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static ZelleAccountDto fromBisq2Model(ZelleAccount account) {
        return new ZelleAccountDto(
                account.getAccountName(),
                new ZelleAccountPayloadDto(
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getEmailOrMobileNr()
                ),
                account.getCreationDate()
        );
    }
}
