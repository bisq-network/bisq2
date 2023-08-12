package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.CashByMailAccount;
import bisq.account.protobuf.CashByMailAccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CashByMailAccountTest {

    private static final bisq.account.protobuf.Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(
                    PaymentMethod.newBuilder()
                            .setName("CASH_BY_MAIL")
                            .setFiatPaymentMethod(
                                    FiatPaymentMethod.newBuilder()))
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("CASH_BY_MAIL")
                    .setCashByMailAccountPayload(CashByMailAccountPayload.newBuilder()
                            .setPostalAddress("postalAddress")
                            .setContact("contact")
                            .setExtraInfo("extraInfo")))
            .setCashByMailAccount(CashByMailAccount.newBuilder())
            .build();

    private static final bisq.account.accounts.CashByMailAccount ACCOUNT = new bisq.account.accounts.CashByMailAccount(
            123,
            "accountName",
            new bisq.account.accounts.CashByMailAccountPayload("id", "CASH_BY_MAIL", "postalAddress", "contact", "extraInfo")
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
        var result = bisq.account.accounts.CashByMailAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(ACCOUNT);
    }
}