package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.CashByMailAccount;
import bisq.account.accounts.fiat.CashByMailAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.CashByMailAccountDto;
import bisq.api.dto.account.fiat.CashByMailAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class CashByMailAccountDtoMapping {
    public static CashByMailAccount toBisq2Model(CashByMailAccountDto dto) {
        CashByMailAccountPayloadDto payloadDto = dto.accountPayload();
        CashByMailAccountPayload payload = new CashByMailAccountPayload(
                StringUtils.createUid(),
                payloadDto.selectedCurrencyCode(),
                payloadDto.postalAddress(),
                payloadDto.contact(),
                payloadDto.extraInfo()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new CashByMailAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static CashByMailAccountDto fromBisq2Model(CashByMailAccount account) {
        return new CashByMailAccountDto(
                account.getAccountName(),
                new CashByMailAccountPayloadDto(
                        account.getAccountPayload().getSelectedCurrencyCode(),
                        account.getAccountPayload().getPostalAddress(),
                        account.getAccountPayload().getContact(),
                        account.getAccountPayload().getExtraInfo()
                )
        );
    }
}
