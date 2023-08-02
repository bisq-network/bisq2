package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.CountryBasedAccountPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class F2FAccountPayloadTest {

    private static final AccountPayload PROTO = AccountPayload.newBuilder()
            .setId("id")
            .setPaymentMethodName("paymentMethodName")
            .setCountryBasedAccountPayload(
                    CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("countryCode")
                            .setF2FAccountPayload(
                                    bisq.account.protobuf.F2FAccountPayload.newBuilder()
                                            .setCity("city")
                                            .setContact("contact")
                                            .setExtraInfo("extraInfo")))
            .build();

    private static final F2FAccountPayload PAYLOAD = new F2FAccountPayload(
            "id", "paymentMethodName", "countryCode",
            "city", "contact", "extraInfo");

    @Test
    void toProto() {
        assertEquals(PROTO, PAYLOAD.toProto());
    }

    @Test
    void fromProto() {
        assertEquals(PAYLOAD, F2FAccountPayload.fromProto(PROTO));
    }
}