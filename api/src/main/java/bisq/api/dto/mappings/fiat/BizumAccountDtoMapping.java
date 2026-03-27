package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.BizumAccount;
import bisq.account.accounts.fiat.BizumAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.BizumAccountDto;
import bisq.api.dto.account.fiat.BizumAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class BizumAccountDtoMapping {
    public static BizumAccount toBisq2Model(BizumAccountDto dto) {
        BizumAccountPayloadDto payloadDto = dto.accountPayload();
        BizumAccountPayload payload = new BizumAccountPayload(
                StringUtils.createUid(),
                payloadDto.countryCode(),
                payloadDto.mobileNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new BizumAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static BizumAccountDto fromBisq2Model(BizumAccount account) {
        return new BizumAccountDto(
                account.getAccountName(),
                new BizumAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getMobileNr()
                )
        );
    }
}
