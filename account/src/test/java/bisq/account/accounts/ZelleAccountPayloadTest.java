package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.ZelleAccountPayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class ZelleAccountPayloadTest {

    private static final AccountPayload PROTO = AccountPayload.newBuilder()
            .setId("id")
            .setPaymentMethodName("ZELLE")
            .setZelleAccountPayload(ZelleAccountPayload.newBuilder()
                    .setEmailOrMobileNr("email")
                    .setHolderName("holderName"))
            .build();

    private static final bisq.account.accounts.ZelleAccountPayload ACCOUNT = new bisq.account.accounts.ZelleAccountPayload(
            "id", "ZELLE", "email", "holderName");

    @Test
    void toProto() {
        var result = ACCOUNT.toProto();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(PROTO);
    }

    @Test
    void fromProto() {
        var result = bisq.account.accounts.ZelleAccountPayload.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(ACCOUNT);
    }
}