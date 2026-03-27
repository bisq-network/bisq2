package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.MoneseAccount;
import bisq.account.accounts.fiat.MoneseAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.MoneseAccountDto;
import bisq.api.dto.account.fiat.MoneseAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class MoneseAccountDtoMapping {
    public static MoneseAccount toBisq2Model(MoneseAccountDto dto) {
        MoneseAccountPayloadDto payloadDto = dto.accountPayload();
        MoneseAccountPayload payload = new MoneseAccountPayload(
                StringUtils.createUid(),
                payloadDto.selectedCurrencyCodes(),
                payloadDto.holderName(),
                payloadDto.mobileNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new MoneseAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static MoneseAccountDto fromBisq2Model(MoneseAccount account) {
        return new MoneseAccountDto(
                account.getAccountName(),
                new MoneseAccountPayloadDto(
                        account.getAccountPayload().getSelectedCurrencyCodes(),
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getMobileNr()
                )
        );
    }
}
