package bisq.api.dto.account.create;

import bisq.api.dto.account.payment_rail.PaymentRailDto;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = CreatePaymentAccountDtoDeserializer.class)
public record CreatePaymentAccountDto(
        String accountName,
        PaymentRailDto paymentRail,
        CreatePaymentAccountPayloadDto accountPayload
) {
}
