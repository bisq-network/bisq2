package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.CountryBasedAccount;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import bisq.account.protobuf.SepaInstantAccount;
import bisq.account.protobuf.SepaInstantAccountPayload;
import bisq.common.protobuf.Country;
import bisq.common.protobuf.Region;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class SepaInstantAccountTest {

    private static final bisq.account.protobuf.Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(PaymentMethod.newBuilder()
                    .setName("SEPA_INSTANT")
                    .setFiatPaymentMethod(FiatPaymentMethod.newBuilder()))
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("SEPA_INSTANT")
                    .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("DE")
                            .setSepaInstantAccountPayload(SepaInstantAccountPayload.newBuilder()
                                    .setHolderName("holderName")
                                    .setIban("DE89370400440532013000")
                                    .setBic("DEUTDEBBXXX")
                                    .addAllAcceptedCountryCodes(List.of("DE", "FR", "IT")))))
            .setCountryBasedAccount(CountryBasedAccount.newBuilder()
                    .setSepaInstantAccount(SepaInstantAccount.newBuilder()))
            .build();

    private static final bisq.account.accounts.SepaInstantAccount ACCOUNT = new bisq.account.accounts.SepaInstantAccount(
            "accountName",
            new bisq.account.accounts.SepaInstantAccountPayload(
                    "id",
                    "SEPA_INSTANT",
                    "holderName",
                    "DE89370400440532013000",
                    "DEUTDEBBXXX",
                    "DE",
                    List.of("DE", "FR", "IT")));

    @Test
    void testToProto() {
        var result = ACCOUNT.completeProto();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate_", "accountPayload_.memoizedHashCode", "memoizedHashCode")
                .isEqualTo(PROTO);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
    }

    @Test
    void testFromProto() {
        var result = bisq.account.accounts.SepaInstantAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
    }
}