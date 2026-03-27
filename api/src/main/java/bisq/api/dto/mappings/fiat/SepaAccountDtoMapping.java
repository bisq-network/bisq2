package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.SepaAccount;
import bisq.account.accounts.fiat.SepaAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.SepaAccountDto;
import bisq.api.dto.account.fiat.SepaAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class SepaAccountDtoMapping {
    public static SepaAccount toBisq2Model(SepaAccountDto dto) {
        SepaAccountPayloadDto payloadDto = dto.accountPayload();
        SepaAccountPayload payload = new SepaAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.iban(),
                payloadDto.bic(),
                payloadDto.countryCode(),
                payloadDto.acceptedCountryCodes()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new SepaAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static SepaAccountDto fromBisq2Model(SepaAccount account) {
        return new SepaAccountDto(
                account.getAccountName(),
                new SepaAccountPayloadDto(
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getIban(),
                        account.getAccountPayload().getBic(),
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getAcceptedCountryCodes()
                )
        );
    }
}
