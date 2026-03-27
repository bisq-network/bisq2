package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.SepaInstantAccount;
import bisq.account.accounts.fiat.SepaInstantAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.SepaInstantAccountDto;
import bisq.api.dto.account.fiat.SepaInstantAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;
import java.util.List;

public class SepaInstantAccountDtoMapping {
    public static SepaInstantAccount toBisq2Model(SepaInstantAccountDto dto) {
        SepaInstantAccountPayloadDto payloadDto = dto.accountPayload();
        SepaInstantAccountPayload payload = new SepaInstantAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.iban(),
                payloadDto.bic(),
                payloadDto.countryCode(),
                payloadDto.acceptedCountryCodes()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new SepaInstantAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static SepaInstantAccountDto fromBisq2Model(SepaInstantAccount account) {
        return new SepaInstantAccountDto(
                account.getAccountName(),
                new SepaInstantAccountPayloadDto(
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getIban(),
                        account.getAccountPayload().getBic(),
                        account.getAccountPayload().getCountryCode(),
                        List.copyOf(account.getAccountPayload().getAcceptedCountryCodes())
                )
        );
    }
}
