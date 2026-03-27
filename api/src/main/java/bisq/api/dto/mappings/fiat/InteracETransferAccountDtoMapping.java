package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.InteracETransferAccount;
import bisq.account.accounts.fiat.InteracETransferAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.InteracETransferAccountDto;
import bisq.api.dto.account.fiat.InteracETransferAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class InteracETransferAccountDtoMapping {
    public static InteracETransferAccount toBisq2Model(InteracETransferAccountDto dto) {
        InteracETransferAccountPayloadDto payloadDto = dto.accountPayload();
        InteracETransferAccountPayload payload = new InteracETransferAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.email(),
                payloadDto.question(),
                payloadDto.answer()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new InteracETransferAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static InteracETransferAccountDto fromBisq2Model(InteracETransferAccount account) {
        return new InteracETransferAccountDto(
                account.getAccountName(),
                new InteracETransferAccountPayloadDto(
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getEmail(),
                        account.getAccountPayload().getQuestion(),
                        account.getAccountPayload().getAnswer()
                )
        );
    }
}
