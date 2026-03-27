package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.SbpAccount;
import bisq.account.accounts.fiat.SbpAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.SbpAccountDto;
import bisq.api.dto.account.fiat.SbpAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class SbpAccountDtoMapping {
    public static SbpAccount toBisq2Model(SbpAccountDto dto) {
        SbpAccountPayloadDto payloadDto = dto.accountPayload();
        SbpAccountPayload payload = new SbpAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.mobileNumber(),
                payloadDto.bankName()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new SbpAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static SbpAccountDto fromBisq2Model(SbpAccount account) {
        return new SbpAccountDto(
                account.getAccountName(),
                new SbpAccountPayloadDto(
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getMobileNumber(),
                        account.getAccountPayload().getBankName()
                )
        );
    }
}
