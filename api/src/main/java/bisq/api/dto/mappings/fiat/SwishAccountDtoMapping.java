package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.SwishAccount;
import bisq.account.accounts.fiat.SwishAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.SwishAccountDto;
import bisq.api.dto.account.fiat.SwishAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class SwishAccountDtoMapping {
    public static SwishAccount toBisq2Model(SwishAccountDto dto) {
        SwishAccountPayloadDto payloadDto = dto.accountPayload();
        SwishAccountPayload payload = new SwishAccountPayload(
                StringUtils.createUid(),
                payloadDto.countryCode(),
                payloadDto.holderName(),
                payloadDto.mobileNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new SwishAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static SwishAccountDto fromBisq2Model(SwishAccount account) {
        return new SwishAccountDto(
                account.getAccountName(),
                new SwishAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getMobileNr()
                )
        );
    }
}
