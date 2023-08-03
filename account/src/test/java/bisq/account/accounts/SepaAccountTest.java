package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.CountryBasedAccount;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import bisq.account.protobuf.SepaAccount;
import bisq.account.protobuf.SepaAccountPayload;
import bisq.common.protobuf.Country;
import bisq.common.protobuf.Region;
import org.junit.jupiter.api.Test;

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
                            .setCountryCode("countryCode")
                            .setSepaAccountPayload(SepaAccountPayload.newBuilder()
                                    .setHolderName("holderName")
                                    .setIban("iban")
                                    .setBic("bic")
                            )))
            .setCountryBasedAccount(CountryBasedAccount.newBuilder()
                    .setCountry(Country.newBuilder()
                            .setCode("countryCode")
                            .setName("countryName")
                            .setRegion(Region.newBuilder()
                                    .setCode("regionCode")
                                    .setName("regionName")))
                    .setSepaAccount(SepaAccount.newBuilder()))
            .build();

    private static final bisq.account.accounts.SepaAccount ACCOUNT = new bisq.account.accounts.SepaAccount(
            "accountName",
            "holderName",
            "iban",
            "bic",
            new bisq.common.locale.Country(
                    "countryCode",
                    "countryName",
                    new bisq.common.locale.Region("regionCode", "regionName")));

    @Test
    void toProto() {
        var result = ACCOUNT.toProto();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate_", "accountPayload_.id_", "memoizedHashCode", "accountPayload_.memoizedHashCode")
                .isEqualTo(PROTO);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
        assertThat(result.getAccountPayload().getId()).isNotEmpty();
    }

    @Test
    void fromProto() {
        var result = bisq.account.accounts.SepaAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate", "accountPayload.id")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
        assertThat(result.getAccountPayload().getId()).isNotEmpty();
    }
}
