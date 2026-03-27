package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.PromptPayAccount;
import bisq.account.accounts.fiat.PromptPayAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.PromptPayAccountDto;
import bisq.api.dto.account.fiat.PromptPayAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class PromptPayAccountDtoMapping {
    public static PromptPayAccount toBisq2Model(PromptPayAccountDto dto) {
        PromptPayAccountPayloadDto payloadDto = dto.accountPayload();
        PromptPayAccountPayload payload = new PromptPayAccountPayload(
                StringUtils.createUid(),
                payloadDto.countryCode(),
                payloadDto.promptPayId()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new PromptPayAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PromptPayAccountDto fromBisq2Model(PromptPayAccount account) {
        return new PromptPayAccountDto(
                account.getAccountName(),
                new PromptPayAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getPromptPayId()
                )
        );
    }
}
