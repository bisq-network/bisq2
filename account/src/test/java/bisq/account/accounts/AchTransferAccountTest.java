package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.BankAccount;
import bisq.account.protobuf.BankAccountPayload;
import bisq.account.protobuf.CountryBasedAccount;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class AchTransferAccountTest {

    private static final Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(PaymentMethod.newBuilder()
                    .setName("ACH_TRANSFER")
                    .setFiatPaymentMethod(FiatPaymentMethod.newBuilder()))
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("ACH_TRANSFER")
                    .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("US")
                            .setBankAccountPayload(BankAccountPayload.newBuilder()
                                    .setBankName("bankName")
                                    .setAchTransferAccountPayload(
                                            bisq.account.protobuf.AchTransferAccountPayload.newBuilder())))
            )
            .setCountryBasedAccount(CountryBasedAccount.newBuilder()
                    .setBankAccount(BankAccount.newBuilder()
                            .setAchTransferAccount(bisq.account.protobuf.AchTransferAccount.newBuilder())))
            .build();

    private static final AchTransferAccount ACCOUNT = new AchTransferAccount(
            "accountName",
            new AchTransferAccountPayload("id", "ACH_TRANSFER",
                    "US", null, Optional.of("bankName"),
                    null, null, null, null));

    @Test
    void testToProto() {
        var result = ACCOUNT.completeProto();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate_", "memoizedHashCode")
                .isEqualTo(PROTO);
    }

    @Test
    void testFromProto() {
        var result = AchTransferAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
    }
}
