package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.PayseraAccount;
import bisq.account.accounts.fiat.PayseraAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.PayseraAccountDto;
import bisq.api.dto.account.fiat.PayseraAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class PayseraAccountDtoMapping {
    public static PayseraAccount toBisq2Model(PayseraAccountDto dto) {
        PayseraAccountPayloadDto payloadDto = dto.accountPayload();
        PayseraAccountPayload payload = new PayseraAccountPayload(
                StringUtils.createUid(),
                payloadDto.selectedCurrencyCodes(),
                payloadDto.email()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new PayseraAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PayseraAccountDto fromBisq2Model(PayseraAccount account) {
        return new PayseraAccountDto(
                account.getAccountName(),
                new PayseraAccountPayloadDto(
                        account.getAccountPayload().getSelectedCurrencyCodes(),
                        account.getAccountPayload().getEmail()
                )
        );
    }
}
