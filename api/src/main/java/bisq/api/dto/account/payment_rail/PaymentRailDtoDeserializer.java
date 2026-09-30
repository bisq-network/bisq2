package bisq.api.dto.account.payment_rail;

import bisq.api.dto.account.crypto.common.CryptoPaymentRailDto;
import bisq.api.dto.account.fiat.common.FiatPaymentRailDto;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class PaymentRailDtoDeserializer extends JsonDeserializer<PaymentRailDto> {
    @Override
    public PaymentRailDto deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return fromString(parser.getValueAsString(), context);
    }

    public static PaymentRailDto fromString(String value, DeserializationContext context) throws IOException {
        if (value == null || value.isBlank()) {
            throw context.weirdStringException(value, PaymentRailDto.class, "Payment rail must not be empty");
        }

        try {
            return FiatPaymentRailDto.valueOf(value);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            return CryptoPaymentRailDto.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            throw context.weirdStringException(value, PaymentRailDto.class, "Unsupported payment rail");
        }
    }
}
