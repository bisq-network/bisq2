package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.BankAccount;
import bisq.account.protobuf.BankAccountPayload;
import bisq.account.protobuf.CashDepositAccount;
import bisq.account.protobuf.CashDepositAccountPayload;
import bisq.account.protobuf.CountryBasedAccount;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.*;
import bisq.common.protobuf.Country;
import bisq.common.protobuf.Region;
import org.junit.jupiter.api.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class CashDepositAccountTest {

    private static final bisq.account.protobuf.Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(System.currentTimeMillis())
            .setPaymentMethod(PaymentMethod.newBuilder()
                    .setName("CASH_DEPOSIT")
                    .setFiatPaymentMethod(FiatPaymentMethod.newBuilder()))
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("CASH_DEPOSIT")
                    .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("US")
                            .setBankAccountPayload(BankAccountPayload.newBuilder()
                                    .setHolderName("holderName")
                                    .setAccountNr("accountNr")
                                    .setAccountType("accountType")
                                    .setBankName("bankName")
                                    .setBranchId("branchId")
                                    .setHolderTaxId("holderTaxId")
                                    .setBankId("bankId")
                                    .setNationalAccountId("nationalAccountId")
                                    .setCashDepositAccountPayload(CashDepositAccountPayload.newBuilder()
                                            .setRequirements("requirements")
                                            .build())))
            )
            .setCountryBasedAccount(CountryBasedAccount.newBuilder()
                    .setCountry(Country.newBuilder()
                            .setCode("US")
                            .setName("countryName")
                            .setRegion(Region.newBuilder()
                                    .setCode("regionCode")
                                    .setName("regionName")))
                    .setBankAccount(BankAccount.newBuilder()
                            .setCashDepositAccount(CashDepositAccount.newBuilder().build())))
            .build();

    private static final bisq.account.accounts.CashDepositAccount ACCOUNT = new bisq.account.accounts.CashDepositAccount(
            "accountName",
            new bisq.account.accounts.CashDepositAccountPayload(
                    "id", "CASH_DEPOSIT", "US",
                    "holderName", "bankName", "branchId",
                    "accountNr", "accountType", "holderTaxId",
                    "bankId", "nationalAccountId", "requirements"),
            new bisq.common.locale.Country(
                    "US",
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
        var result = bisq.account.accounts.CashDepositAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
    }
}