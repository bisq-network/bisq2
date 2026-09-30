package bisq.api.dto.account.fiat.zelle;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;

public record CreateZelleAccountPayloadDto(
        String holderName,
        String emailOrMobileNr
) implements CreatePaymentAccountPayloadDto {
}
