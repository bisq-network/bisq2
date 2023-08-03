package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import bisq.account.protobuf.RevolutAccount;
import bisq.account.protobuf.RevolutAccountPayload;
import org.junit.jupiter.api.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class RevolutAccountTest {

    private static final Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(
                    PaymentMethod.newBuilder()
                            .setName("REVOLUT")
                            .setFiatPaymentMethod(
                                    FiatPaymentMethod.newBuilder().build())
                            .build())
            .setRevolutAccount(RevolutAccount.newBuilder())
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("REVOLUT")
                    .setRevolutAccountPayload(RevolutAccountPayload.newBuilder()
                            .setEmail("email")
                            .build())
                    .build())
            .build();

    private static final bisq.account.accounts.RevolutAccount ACCOUNT = new bisq.account.accounts.RevolutAccount(
            "accountName", "email");

    @Test
    void toProto() {
        var result = ACCOUNT.toProto();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("accountPayload_.id_", "accountPayload_.memoizedHashCode", "creationDate_", "memoizedHashCode")
                .isEqualTo(PROTO);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
        assertThat(result.getAccountPayload().getId()).isNotEmpty();
    }

    @Test
    void fromProto() {
        var result = bisq.account.accounts.RevolutAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate", "accountPayload.id")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
        assertThat(result.getAccountPayload().getId()).isNotEmpty();
    }
}