package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.PerfectMoneyAccount;
import bisq.account.accounts.fiat.PerfectMoneyAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.PerfectMoneyAccountDto;
import bisq.api.dto.account.fiat.PerfectMoneyAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class PerfectMoneyAccountDtoMapping {
    public static PerfectMoneyAccount toBisq2Model(PerfectMoneyAccountDto dto) {
        PerfectMoneyAccountPayloadDto payloadDto = dto.accountPayload();
        PerfectMoneyAccountPayload payload = new PerfectMoneyAccountPayload(
                StringUtils.createUid(),
                payloadDto.selectedCurrencyCode(),
                payloadDto.accountNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new PerfectMoneyAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PerfectMoneyAccountDto fromBisq2Model(PerfectMoneyAccount account) {
        return new PerfectMoneyAccountDto(
                account.getAccountName(),
                new PerfectMoneyAccountPayloadDto(
                        account.getAccountPayload().getSelectedCurrencyCode(),
                        account.getAccountPayload().getAccountNr()
                )
        );
    }
}
