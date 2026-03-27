package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.SatispayAccount;
import bisq.account.accounts.fiat.SatispayAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.SatispayAccountDto;
import bisq.api.dto.account.fiat.SatispayAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class SatispayAccountDtoMapping {
    public static SatispayAccount toBisq2Model(SatispayAccountDto dto) {
        SatispayAccountPayloadDto payloadDto = dto.accountPayload();
        SatispayAccountPayload payload = new SatispayAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.mobileNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new SatispayAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static SatispayAccountDto fromBisq2Model(SatispayAccount account) {
        return new SatispayAccountDto(
                account.getAccountName(),
                new SatispayAccountPayloadDto(
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getMobileNr()
                )
        );
    }
}
