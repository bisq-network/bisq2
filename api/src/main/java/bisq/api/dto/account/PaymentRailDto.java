package bisq.api.dto.account;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = PaymentRailDtoDeserializer.class)
public interface PaymentRailDto {
}
