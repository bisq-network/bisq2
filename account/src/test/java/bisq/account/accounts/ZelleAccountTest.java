package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import bisq.account.protobuf.ZelleAccountPayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ZelleAccountTest {

    private static final bisq.account.protobuf.Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(
                    PaymentMethod.newBuilder()
                            .setName("ZELLE")
                            .setFiatPaymentMethod(
                                    FiatPaymentMethod.newBuilder().build())
                            .build())
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("ZELLE")
                    .setZelleAccountPayload(ZelleAccountPayload.newBuilder()
                            .setEmailOrMobileNr("email")
                            .setHolderName("holderName")
                            .build())
                    .build())
            .setZelleAccount(bisq.account.protobuf.ZelleAccount.newBuilder())
            .build();

    private static final bisq.account.accounts.ZelleAccount ACCOUNT = new bisq.account.accounts.ZelleAccount(
            123, "accountName", new bisq.account.accounts.ZelleAccountPayload("id", "ZELLE", "email", "holderName"));

    @Test
    void toProto() {
        var result = ACCOUNT.toProto();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(PROTO);
    }

    @Test
    void fromProto() {
        var result = ZelleAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(ACCOUNT);
    }
}