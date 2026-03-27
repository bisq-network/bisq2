package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.NeftAccount;
import bisq.account.accounts.fiat.NeftAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.NeftAccountDto;
import bisq.api.dto.account.fiat.NeftAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class NeftAccountDtoMapping {
    public static NeftAccount toBisq2Model(NeftAccountDto dto) {
        NeftAccountPayloadDto payloadDto = dto.accountPayload();
        NeftAccountPayload payload = new NeftAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.accountNr(),
                payloadDto.ifsc()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new NeftAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static NeftAccountDto fromBisq2Model(NeftAccount account) {
        return new NeftAccountDto(
                account.getAccountName(),
                new NeftAccountPayloadDto(
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getAccountNr(),
                        account.getAccountPayload().getIfsc()
                )
        );
    }
}
