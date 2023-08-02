package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.RevolutAccountPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RevolutAccountPayloadTest {

    private static final AccountPayload PROTO = AccountPayload.newBuilder()
            .setId("id")
            .setPaymentMethodName("paymentMethodName")
            .setRevolutAccountPayload(RevolutAccountPayload.newBuilder()
                    .setEmail("email"))
            .build();

    private static final bisq.account.accounts.RevolutAccountPayload PAYLOAD = new bisq.account.accounts.RevolutAccountPayload(
            "id", "paymentMethodName", "email");

    @Test
    void toProto() {
        assertEquals(PROTO, PAYLOAD.toProto());
    }

    @Test
    void fromProto() {
        assertEquals(PAYLOAD, bisq.account.accounts.RevolutAccountPayload.fromProto(PROTO));
    }
}
