package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.CountryBasedAccount;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import bisq.account.protobuf.SepaAccount;
import bisq.account.protobuf.SepaAccountPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class SepaAccountTest {

    private static final bisq.account.protobuf.Account PROTO = bisq.account.protobuf.Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(PaymentMethod.newBuilder()
                    .setName("SEPA")
                    .setFiatPaymentMethod(FiatPaymentMethod.newBuilder()))
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("SEPA")
                    .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("DE")
                            .setSepaAccountPayload(SepaAccountPayload.newBuilder()
                                    .setHolderName("holderName")
                                    .setIban("DE89370400440532013000")
                                    .setBic("DEUTDEFFXXX")
                                    .addAllAcceptedCountryCodes(List.of("DE", "FR", "IT"))
                            )))
            .setCountryBasedAccount(CountryBasedAccount.newBuilder()
                    .setSepaAccount(SepaAccount.newBuilder()))
            .build();

    private static final bisq.account.accounts.SepaAccount ACCOUNT =
            new bisq.account.accounts.SepaAccount(
                    "accountName",
                    new bisq.account.accounts.SepaAccountPayload("id",
                            bisq.account.payment_method.FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA).getName(),
                            "holderName",
                            "DE89370400440532013000",
                            "DEUTDEFFXXX",
                            "DE",
                            List.of("DE", "FR", "IT"))
            );

    @Test
    void testToProto() {
        var result = ACCOUNT.completeProto();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate_", "accountPayload_.id_", "memoizedHashCode", "accountPayload_.memoizedHashCode")
                .isEqualTo(PROTO);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
        assertThat(result.getAccountPayload().getId()).isNotEmpty();
    }

    @Test
    void testFromProto() {
        var result = bisq.account.accounts.SepaAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate", "accountPayload.id")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
        assertThat(result.getAccountPayload().getId()).isNotEmpty();
    }
}
