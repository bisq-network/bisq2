package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.BankAccount;
import bisq.account.protobuf.BankAccountPayload;
import bisq.account.protobuf.CountryBasedAccount;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.*;
import bisq.common.protobuf.Country;
import bisq.common.protobuf.Region;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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
                    .setPaymentRailName("NATIONAL_BANK")
                    .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("US")
                            .setBankAccountPayload(BankAccountPayload.newBuilder()
                                    .setBankName("bankName")
                                    .setNationalBankAccountPayload(
                                            bisq.account.protobuf.NationalBankAccountPayload.newBuilder())))
            )
            .setCountryBasedAccount(CountryBasedAccount.newBuilder()
                    .setBankAccount(BankAccount.newBuilder()
                            .setNationalBankAccount(bisq.account.protobuf.NationalBankAccount.newBuilder())))
            .build();

    private static final NationalBankAccount ACCOUNT = new NationalBankAccount(
            "accountName",
            new NationalBankAccountPayload(
                    "id", "NATIONAL_BANK", "US",
                    Optional.empty(), Optional.of("bankName"), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty()));

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
        var result = NationalBankAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
    }
}