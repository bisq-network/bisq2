package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.HalCashAccount;
import bisq.account.accounts.fiat.HalCashAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.HalCashAccountDto;
import bisq.api.dto.account.fiat.HalCashAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class HalCashAccountDtoMapping {
    public static HalCashAccount toBisq2Model(HalCashAccountDto dto) {
        HalCashAccountPayloadDto payloadDto = dto.accountPayload();
        HalCashAccountPayload payload = new HalCashAccountPayload(
                StringUtils.createUid(),
                payloadDto.mobileNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new HalCashAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static HalCashAccountDto fromBisq2Model(HalCashAccount account) {
        return new HalCashAccountDto(
                account.getAccountName(),
                new HalCashAccountPayloadDto(
                        account.getAccountPayload().getMobileNr()
                )
        );
    }
}
