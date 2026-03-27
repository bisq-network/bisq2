package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.MercadoPagoAccount;
import bisq.account.accounts.fiat.MercadoPagoAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.MercadoPagoAccountDto;
import bisq.api.dto.account.fiat.MercadoPagoAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class MercadoPagoAccountDtoMapping {
    public static MercadoPagoAccount toBisq2Model(MercadoPagoAccountDto dto) {
        MercadoPagoAccountPayloadDto payloadDto = dto.accountPayload();
        MercadoPagoAccountPayload payload = new MercadoPagoAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.holderId()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new MercadoPagoAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static MercadoPagoAccountDto fromBisq2Model(MercadoPagoAccount account) {
        return new MercadoPagoAccountDto(
                account.getAccountName(),
                new MercadoPagoAccountPayloadDto(
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getHolderId()
                )
        );
    }
}
