package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PayIDAccount;
import bisq.account.protobuf.PayIDAccountPayload;
import bisq.account.protobuf.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PayIDAccountTest {

    private static final bisq.account.protobuf.Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(
                    PaymentMethod.newBuilder()
                            .setName("PAY_ID")
                            .setFiatPaymentMethod(
                                    FiatPaymentMethod.newBuilder()))
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("PAY_ID")
                    .setPayIDAccountPayload(PayIDAccountPayload.newBuilder()
                            .setBankAccountName("bankAccountName")
                            .setPayId("payId")))
            .setPayIDAccount(PayIDAccount.newBuilder())
            .build();

    private static final bisq.account.accounts.PayIDAccount ACCOUNT = new bisq.account.accounts.PayIDAccount(
            123,
            "accountName",
            new bisq.account.accounts.PayIDAccountPayload("id", "PAY_ID", "bankAccountName", "payId")
    );

    @Test
    void toProto() {
        var result = ACCOUNT.toProto();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(PROTO);
    }

    @Test
    void fromProto() {
        var result = bisq.account.accounts.PayIDAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(ACCOUNT);
    }
}