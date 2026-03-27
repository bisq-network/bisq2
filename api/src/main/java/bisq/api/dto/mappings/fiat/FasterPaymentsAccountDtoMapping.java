package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.FasterPaymentsAccount;
import bisq.account.accounts.fiat.FasterPaymentsAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.FasterPaymentsAccountDto;
import bisq.api.dto.account.fiat.FasterPaymentsAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class FasterPaymentsAccountDtoMapping {
    public static FasterPaymentsAccount toBisq2Model(FasterPaymentsAccountDto dto) {
        FasterPaymentsAccountPayloadDto payloadDto = dto.accountPayload();
        FasterPaymentsAccountPayload payload = new FasterPaymentsAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.sortCode(),
                payloadDto.accountNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new FasterPaymentsAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static FasterPaymentsAccountDto fromBisq2Model(FasterPaymentsAccount account) {
        return new FasterPaymentsAccountDto(
                account.getAccountName(),
                new FasterPaymentsAccountPayloadDto(
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getSortCode(),
                        account.getAccountPayload().getAccountNr()
                )
        );
    }
}
