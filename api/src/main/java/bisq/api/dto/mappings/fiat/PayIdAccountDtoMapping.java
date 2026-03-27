package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.PayIdAccount;
import bisq.account.accounts.fiat.PayIdAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.PayIdAccountDto;
import bisq.api.dto.account.fiat.PayIdAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class PayIdAccountDtoMapping {
    public static PayIdAccount toBisq2Model(PayIdAccountDto dto) {
        PayIdAccountPayloadDto payloadDto = dto.accountPayload();
        PayIdAccountPayload payload = new PayIdAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.payId()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new PayIdAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PayIdAccountDto fromBisq2Model(PayIdAccount account) {
        return new PayIdAccountDto(
                account.getAccountName(),
                new PayIdAccountPayloadDto(
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getPayId()
                )
        );
    }
}
