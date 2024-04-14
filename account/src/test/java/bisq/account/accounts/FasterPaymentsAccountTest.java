package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.FasterPaymentsAccount;
import bisq.account.protobuf.FasterPaymentsAccountPayload;
import bisq.account.protobuf.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FasterPaymentsAccountTest {

    private static final bisq.account.protobuf.Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(
                    PaymentMethod.newBuilder()
                            .setName("FASTER_PAYMENTS")
                            .setFiatPaymentMethod(
                                    FiatPaymentMethod.newBuilder()))
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("FASTER_PAYMENTS")
                    .setFasterPaymentsAccountPayload(FasterPaymentsAccountPayload.newBuilder()
                            .setSortCode("sortCode")
                            .setAccountNr("accountNr")))
            .setFasterPaymentsAccount(FasterPaymentsAccount.newBuilder())
            .build();

    private static final bisq.account.accounts.FasterPaymentsAccount ACCOUNT = new bisq.account.accounts.FasterPaymentsAccount(
            123,
            "accountName",
            new bisq.account.accounts.FasterPaymentsAccountPayload("id", "FASTER_PAYMENTS", "sortCode", "accountNr")
    );

    @Test
    void testToProto() {
        var result = ACCOUNT.writeProto();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(PROTO);
    }

    @Test
    void testFromProto() {
        var result = bisq.account.accounts.FasterPaymentsAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(ACCOUNT);
    }
}