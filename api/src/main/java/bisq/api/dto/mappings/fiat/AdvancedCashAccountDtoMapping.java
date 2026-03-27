package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.AdvancedCashAccount;
import bisq.account.accounts.fiat.AdvancedCashAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.AdvancedCashAccountDto;
import bisq.api.dto.account.fiat.AdvancedCashAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class AdvancedCashAccountDtoMapping {
    public static AdvancedCashAccount toBisq2Model(AdvancedCashAccountDto dto) {
        AdvancedCashAccountPayloadDto payloadDto = dto.accountPayload();
        AdvancedCashAccountPayload payload = new AdvancedCashAccountPayload(
                StringUtils.createUid(),
                payloadDto.selectedCurrencyCodes(),
                payloadDto.accountNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new AdvancedCashAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static AdvancedCashAccountDto fromBisq2Model(AdvancedCashAccount account) {
        return new AdvancedCashAccountDto(
                account.getAccountName(),
                new AdvancedCashAccountPayloadDto(
                        account.getAccountPayload().getSelectedCurrencyCodes(),
                        account.getAccountPayload().getAccountNr()
                )
        );
    }
}
