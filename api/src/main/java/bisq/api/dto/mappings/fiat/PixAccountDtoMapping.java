package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.PixAccount;
import bisq.account.accounts.fiat.PixAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.PixAccountDto;
import bisq.api.dto.account.fiat.PixAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class PixAccountDtoMapping {
    public static PixAccount toBisq2Model(PixAccountDto dto) {
        PixAccountPayloadDto payloadDto = dto.accountPayload();
        PixAccountPayload payload = new PixAccountPayload(
                StringUtils.createUid(),
                payloadDto.countryCode(),
                payloadDto.holderName(),
                payloadDto.pixKey()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new PixAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PixAccountDto fromBisq2Model(PixAccount account) {
        return new PixAccountDto(
                account.getAccountName(),
                new PixAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getPixKey()
                )
        );
    }
}
