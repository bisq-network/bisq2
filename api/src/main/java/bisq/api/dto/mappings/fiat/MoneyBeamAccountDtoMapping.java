package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.MoneyBeamAccount;
import bisq.account.accounts.fiat.MoneyBeamAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.MoneyBeamAccountDto;
import bisq.api.dto.account.fiat.MoneyBeamAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class MoneyBeamAccountDtoMapping {
    public static MoneyBeamAccount toBisq2Model(MoneyBeamAccountDto dto) {
        MoneyBeamAccountPayloadDto payloadDto = dto.accountPayload();
        MoneyBeamAccountPayload payload = new MoneyBeamAccountPayload(
                StringUtils.createUid(),
                payloadDto.countryCode(),
                payloadDto.holderName(),
                payloadDto.emailOrMobileNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new MoneyBeamAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static MoneyBeamAccountDto fromBisq2Model(MoneyBeamAccount account) {
        return new MoneyBeamAccountDto(
                account.getAccountName(),
                new MoneyBeamAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getEmailOrMobileNr()
                )
        );
    }
}
