package bisq.api.dto.account.payment_rail;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = PaymentRailDtoDeserializer.class)
public interface PaymentRailDto {
}
