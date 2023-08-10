package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.BankAccount;
import bisq.account.protobuf.BankAccountPayload;
import bisq.account.protobuf.CountryBasedAccount;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import bisq.common.protobuf.Country;
import bisq.common.protobuf.Region;
import org.junit.jupiter.api.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class NationalBankAccountTest {

    private static final bisq.account.protobuf.Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(PaymentMethod.newBuilder()
                    .setName("NATIONAL_BANK")
                    .setFiatPaymentMethod(FiatPaymentMethod.newBuilder()))
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("NATIONAL_BANK")
                    .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("countryCode")
                            .setBankAccountPayload(BankAccountPayload.newBuilder()
                                    .setBankName("bankName")
                                    .setNationalBankAccountPayload(
                                            bisq.account.protobuf.NationalBankAccountPayload.newBuilder())))
            )
            .setCountryBasedAccount(CountryBasedAccount.newBuilder()
                    .setCountry(Country.newBuilder()
                            .setCode("countryCode")
                            .setName("countryName")
                            .setRegion(Region.newBuilder()
                                    .setCode("regionCode")
                                    .setName("regionName")))
                    .setBankAccount(BankAccount.newBuilder()
                            .setNationalBankAccount(bisq.account.protobuf.NationalBankAccount.newBuilder())))
            .build();

    private static final NationalBankAccount ACCOUNT = new NationalBankAccount(
            "accountName",
            new NationalBankAccountPayload(
                    "id", "NATIONAL_BANK", "countryCode",
                    null, "bankName", null,
                    null, null, null,
                    null, null),
            new bisq.common.locale.Country(
                    "countryCode",
                    "countryName",
                    new bisq.common.locale.Region("regionCode", "regionName")));

    @Test
    void toProto() {
        var result = ACCOUNT.toProto();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate_", "memoizedHashCode")
                .isEqualTo(PROTO);
    }

    @Test
    void fromProto() {
        var result = NationalBankAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
    }
}