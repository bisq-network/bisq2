package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.MoneyGramAccount;
import bisq.account.accounts.fiat.MoneyGramAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.MoneyGramAccountDto;
import bisq.api.dto.account.fiat.MoneyGramAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class MoneyGramAccountDtoMapping {
    public static MoneyGramAccount toBisq2Model(MoneyGramAccountDto dto) {
        MoneyGramAccountPayloadDto payloadDto = dto.accountPayload();
        MoneyGramAccountPayload payload = new MoneyGramAccountPayload(
                StringUtils.createUid(),
                payloadDto.countryCode(),
                payloadDto.selectedCurrencyCodes(),
                payloadDto.holderName(),
                payloadDto.email(),
                payloadDto.state()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new MoneyGramAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static MoneyGramAccountDto fromBisq2Model(MoneyGramAccount account) {
        return new MoneyGramAccountDto(
                account.getAccountName(),
                new MoneyGramAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getSelectedCurrencyCodes(),
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getEmail(),
                        account.getAccountPayload().getState()
                )
        );
    }
}
