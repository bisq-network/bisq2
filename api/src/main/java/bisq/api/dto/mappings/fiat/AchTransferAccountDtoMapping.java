package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.AchTransferAccount;
import bisq.account.accounts.fiat.AchTransferAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.AchTransferAccountDto;
import bisq.api.dto.account.fiat.AchTransferAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class AchTransferAccountDtoMapping {
    public static AchTransferAccount toBisq2Model(AchTransferAccountDto dto) {
        AchTransferAccountPayloadDto payloadDto = dto.accountPayload();
        AchTransferAccountPayload payload = new AchTransferAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.holderAddress(),
                payloadDto.bankName(),
                payloadDto.routingNr(),
                payloadDto.accountNr(),
                payloadDto.bankAccountType()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new AchTransferAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static AchTransferAccountDto fromBisq2Model(AchTransferAccount account) {
        AchTransferAccountPayload payload = account.getAccountPayload();
        String holderName = payload.getHolderName()
                .orElseThrow(() -> new IllegalStateException("ACH holderName missing"));
        String bankName = payload.getBankName()
                .orElseThrow(() -> new IllegalStateException("ACH bankName missing"));
        String routingNr = payload.getBankId()
                .orElseThrow(() -> new IllegalStateException("ACH routingNr missing"));

        return new AchTransferAccountDto(
                account.getAccountName(),
                new AchTransferAccountPayloadDto(
                        holderName,
                        payload.getHolderAddress(),
                        bankName,
                        routingNr,
                        payload.getAccountNr(),
                        payload.getBankAccountType()
                                .orElseThrow(() -> new IllegalStateException("ACH bankAccountType missing"))
                )
        );
    }
}
