package bisq.api.dto.account.fiat;

import bisq.api.dto.account.PaymentAccountPayloadDto;

public record ZelleAccountPayloadDto(
        String holderName,
        String emailOrMobileNr
) implements PaymentAccountPayloadDto {
}
