package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.SepaAccountPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SepaAccountPayloadTest {

    private static final AccountPayload PROTO = AccountPayload.newBuilder()
            .setId("id")
            .setPaymentMethodName("paymentMethodName")
            .setCountryBasedAccountPayload(
                    CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("countryCode")
                            .setSepaAccountPayload(SepaAccountPayload.newBuilder()
                                    .setHolderName("holderName")
                                    .setIban("iban")
                                    .setBic("bic")))
            .build();

    private static final bisq.account.accounts.SepaAccountPayload PAYLOAD = new bisq.account.accounts.SepaAccountPayload(
            "id", "paymentMethodName", "holderName", "iban", "bic", "countryCode");

    @Test
    void testToProto() {
        assertEquals(PROTO, PAYLOAD.completeProto());
    }

    @Test
    void testFromProto() {
        assertEquals(PAYLOAD, bisq.account.accounts.SepaAccountPayload.fromProto(PROTO));
    }
}
