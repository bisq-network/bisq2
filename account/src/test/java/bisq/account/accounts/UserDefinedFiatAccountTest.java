package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import bisq.account.protobuf.UserDefinedFiatAccount;
import bisq.account.protobuf.UserDefinedFiatAccountPayload;
import org.junit.jupiter.api.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class UserDefinedFiatAccountTest {

    private static final bisq.account.protobuf.Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(
                    PaymentMethod.newBuilder()
                            .setName("accountName")
                            .setFiatPaymentMethod(
                                    FiatPaymentMethod.newBuilder().build())
                            .build())
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("accountName")
                    .setUserDefinedFiatAccountPayload(UserDefinedFiatAccountPayload.newBuilder()
                            .setAccountData("customData")
                            .build())
                    .build())
            .setUserDefinedFiatAccount(UserDefinedFiatAccount.newBuilder().build())
            .build();

    private static final bisq.account.accounts.UserDefinedFiatAccount ACCOUNT = new bisq.account.accounts.UserDefinedFiatAccount(
            "accountName", "customData");

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
        var result = bisq.account.accounts.UserDefinedFiatAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate", "accountPayload.id")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
        assertThat(result.getAccountPayload().getId()).isNotEmpty();
    }
}