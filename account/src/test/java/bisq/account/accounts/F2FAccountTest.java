package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.CountryBasedAccount;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.F2FAccount;
import bisq.account.protobuf.F2FAccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import bisq.common.protobuf.Country;
import bisq.common.protobuf.Region;
import org.junit.jupiter.api.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class F2FAccountTest {

    private static final Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(PaymentMethod.newBuilder()
                    .setName("F2F")
                    .setFiatPaymentMethod(FiatPaymentMethod.newBuilder().build())
                    .build())
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentRailName("F2F")
                    .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("CO")
                            .setF2FAccountPayload(F2FAccountPayload.newBuilder()
                                    .setCity("city")
                                    .setContact("contact")
                                    .setExtraInfo("extraInfo")))
            )
            .setCountryBasedAccount(CountryBasedAccount.newBuilder()
                    .setF2FAccount(F2FAccount.newBuilder())
            )
            .build();

    private static final bisq.account.accounts.F2FAccount ACCOUNT = new bisq.account.accounts.F2FAccount(
            "accountName",
            new bisq.account.accounts.F2FAccountPayload("id", "F2F", "CO", "city", "contact", "extraInfo"));

    @Test
    void testToProto() {
        var result = ACCOUNT.completeProto();
        assertThat(result).usingRecursiveComparison()
                .ignoringFields("accountPayload_.memoizedHashCode", "memoizedHashCode", "creationDate_")
                .isEqualTo(PROTO);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
    }

    @Test
    void testFromProto() {
        var result = bisq.account.accounts.F2FAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(System.currentTimeMillis(), offset(1000L));
    }
}