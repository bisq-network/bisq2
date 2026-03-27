package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.F2FAccount;
import bisq.account.accounts.fiat.F2FAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.F2FAccountDto;
import bisq.api.dto.account.fiat.F2FAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class F2FAccountDtoMapping {
    public static F2FAccount toBisq2Model(F2FAccountDto dto) {
        F2FAccountPayloadDto payloadDto = dto.accountPayload();
        F2FAccountPayload payload = new F2FAccountPayload(
                StringUtils.createUid(),
                payloadDto.countryCode(),
                payloadDto.selectedCurrencyCode(),
                payloadDto.city(),
                payloadDto.contact(),
                payloadDto.extraInfo()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new F2FAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static F2FAccountDto fromBisq2Model(F2FAccount account) {
        return new F2FAccountDto(
                account.getAccountName(),
                new F2FAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getSelectedCurrencyCode(),
                        account.getAccountPayload().getCity(),
                        account.getAccountPayload().getContact(),
                        account.getAccountPayload().getExtraInfo()
                )
        );
    }
}
