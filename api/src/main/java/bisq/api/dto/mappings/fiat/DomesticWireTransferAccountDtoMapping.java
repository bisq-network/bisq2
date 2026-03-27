package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.DomesticWireTransferAccount;
import bisq.account.accounts.fiat.DomesticWireTransferAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.DomesticWireTransferAccountDto;
import bisq.api.dto.account.fiat.DomesticWireTransferAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class DomesticWireTransferAccountDtoMapping {
    public static DomesticWireTransferAccount toBisq2Model(DomesticWireTransferAccountDto dto) {
        DomesticWireTransferAccountPayloadDto payloadDto = dto.accountPayload();
        DomesticWireTransferAccountPayload payload = new DomesticWireTransferAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.holderAddress(),
                payloadDto.bankName(),
                payloadDto.routingNr(),
                payloadDto.accountNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new DomesticWireTransferAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static DomesticWireTransferAccountDto fromBisq2Model(DomesticWireTransferAccount account) {
        DomesticWireTransferAccountPayload payload = account.getAccountPayload();
        String holderName = payload.getHolderName()
                .orElseThrow(() -> new IllegalStateException("DomesticWireTransfer holderName missing"));
        String bankName = payload.getBankName()
                .orElseThrow(() -> new IllegalStateException("DomesticWireTransfer bankName missing"));
        String routingNr = payload.getBankId()
                .orElseThrow(() -> new IllegalStateException("DomesticWireTransfer routingNr missing"));

        return new DomesticWireTransferAccountDto(
                account.getAccountName(),
                new DomesticWireTransferAccountPayloadDto(
                        holderName,
                        payload.getHolderAddress(),
                        bankName,
                        routingNr,
                        payload.getAccountNr()
                )
        );
    }
}
