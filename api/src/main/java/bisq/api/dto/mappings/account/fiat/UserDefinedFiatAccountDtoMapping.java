package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.UserDefinedFiatAccountDto;
import bisq.api.dto.account.fiat.UserDefinedFiatAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class UserDefinedFiatAccountDtoMapping {
    public static UserDefinedFiatAccount toBisq2Model(UserDefinedFiatAccountDto dto) {
        UserDefinedFiatAccountPayloadDto payloadDto = dto.accountPayload();
        UserDefinedFiatAccountPayload payload = new UserDefinedFiatAccountPayload(
                StringUtils.createUid(),
                payloadDto.accountData()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new UserDefinedFiatAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static UserDefinedFiatAccountDto fromBisq2Model(UserDefinedFiatAccount account) {
        return new UserDefinedFiatAccountDto(
                account.getAccountName(),
                new UserDefinedFiatAccountPayloadDto(
                        account.getAccountPayload().getAccountData()
                ),
                account.getCreationDate()
        );
    }
}
