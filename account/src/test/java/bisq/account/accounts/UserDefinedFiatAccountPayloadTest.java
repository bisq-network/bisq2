package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.UserDefinedFiatAccountPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserDefinedFiatAccountPayloadTest {

    private static final AccountPayload PROTO = AccountPayload.newBuilder()
            .setId("id")
            .setPaymentMethodName("paymentMethodName")
            .setUserDefinedFiatAccountPayload(UserDefinedFiatAccountPayload.newBuilder()
                    .setAccountData("custom data"))
            .build();

    private static final bisq.account.accounts.UserDefinedFiatAccountPayload PAYLOAD =
            new bisq.account.accounts.UserDefinedFiatAccountPayload("id", "paymentMethodName", "custom data");

    @Test
    void toProto() {
        assertEquals(PROTO, PAYLOAD.toProto());
    }

    @Test
    void fromProto() {
        assertEquals(PAYLOAD, bisq.account.accounts.UserDefinedFiatAccountPayload.fromProto(PROTO));
    }
}