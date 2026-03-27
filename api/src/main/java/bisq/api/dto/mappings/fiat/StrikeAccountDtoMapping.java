package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.StrikeAccount;
import bisq.account.accounts.fiat.StrikeAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.StrikeAccountDto;
import bisq.api.dto.account.fiat.StrikeAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class StrikeAccountDtoMapping {
    public static StrikeAccount toBisq2Model(StrikeAccountDto dto) {
        StrikeAccountPayloadDto payloadDto = dto.accountPayload();
        StrikeAccountPayload payload = new StrikeAccountPayload(
                StringUtils.createUid(),
                payloadDto.countryCode(),
                payloadDto.holderName()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new StrikeAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static StrikeAccountDto fromBisq2Model(StrikeAccount account) {
        return new StrikeAccountDto(
                account.getAccountName(),
                new StrikeAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getHolderName()
                )
        );
    }
}
