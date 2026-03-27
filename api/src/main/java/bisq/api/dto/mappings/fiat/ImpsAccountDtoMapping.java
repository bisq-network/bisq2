package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.ImpsAccount;
import bisq.account.accounts.fiat.ImpsAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.ImpsAccountDto;
import bisq.api.dto.account.fiat.ImpsAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class ImpsAccountDtoMapping {
    public static ImpsAccount toBisq2Model(ImpsAccountDto dto) {
        ImpsAccountPayloadDto payloadDto = dto.accountPayload();
        ImpsAccountPayload payload = new ImpsAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.accountNr(),
                payloadDto.ifsc()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new ImpsAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static ImpsAccountDto fromBisq2Model(ImpsAccount account) {
        return new ImpsAccountDto(
                account.getAccountName(),
                new ImpsAccountPayloadDto(
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getAccountNr(),
                        account.getAccountPayload().getIfsc()
                )
        );
    }
}
