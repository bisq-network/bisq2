package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import bisq.account.protobuf.USPostalMoneyOrderAccount;
import bisq.account.protobuf.USPostalMoneyOrderAccountPayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class USPostalMoneyOrderAccountTest {

    private static final bisq.account.protobuf.Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(
                    PaymentMethod.newBuilder()
                            .setName("US_POSTAL_MONEY_ORDER")
                            .setFiatPaymentMethod(
                                    FiatPaymentMethod.newBuilder()))
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("US_POSTAL_MONEY_ORDER")
                    .setUsPostalMoneyOrderAccountPayload(USPostalMoneyOrderAccountPayload.newBuilder()
                            .setPostalAddress("postalAddress")
                            .setHolderName("holderName")))
            .setUsPostalMoneyOrderAccount(USPostalMoneyOrderAccount.newBuilder())
            .build();

    private static final bisq.account.accounts.USPostalMoneyOrderAccount ACCOUNT = new bisq.account.accounts.USPostalMoneyOrderAccount(
            123,
            "accountName",
            new bisq.account.accounts.USPostalMoneyOrderAccountPayload("id", "US_POSTAL_MONEY_ORDER", "postalAddress", "holderName")
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
        var result = bisq.account.accounts.USPostalMoneyOrderAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(ACCOUNT);
    }
}