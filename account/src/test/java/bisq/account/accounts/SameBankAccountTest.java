/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.account.accounts;

import bisq.account.protobuf.Account;
import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.BankAccount;
import bisq.account.protobuf.BankAccountPayload;
import bisq.account.protobuf.CountryBasedAccount;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.FiatPaymentMethod;
import bisq.account.protobuf.PaymentMethod;
import bisq.account.protobuf.SameBankAccount;
import bisq.account.protobuf.SameBankAccountPayload;
import bisq.common.protobuf.Country;
import bisq.common.protobuf.Region;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class SameBankAccountTest {

    private static final Account PROTO = Account.newBuilder()
            .setAccountName("accountName")
            .setCreationDate(123)
            .setPaymentMethod(PaymentMethod.newBuilder()
                    .setName("NATIONAL_BANK")
                    .setFiatPaymentMethod(FiatPaymentMethod.newBuilder()))
            .setAccountPayload(AccountPayload.newBuilder()
                    .setId("id")
                    .setPaymentMethodName("NATIONAL_BANK")
                    .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                            .setCountryCode("US")
                            .setBankAccountPayload(BankAccountPayload.newBuilder()
                                    .setHolderName("John Doe")
                                    .setBankName("Test Bank")
                                    .setBranchId("123456")
                                    .setAccountNr("987654321")
                                    .setAccountType("Checking")
                                    .setHolderTaxId("TID12345")
                                    .setBankId("BID12345")
                                    .setNationalAccountId("NAI12345")
                                    .setSameBankAccountPayload(SameBankAccountPayload.newBuilder()))))
            .setCountryBasedAccount(CountryBasedAccount.newBuilder()
                    .setBankAccount(BankAccount.newBuilder()
                            .setSameBankAccount(SameBankAccount.newBuilder())))
            .build();

    private static final bisq.account.accounts.SameBankAccount ACCOUNT = new bisq.account.accounts.SameBankAccount(
            "accountName",
            new bisq.account.accounts.SameBankAccountPayload(
                    "id",
                    "NATIONAL_BANK",
                    "US",
                    Optional.of("John Doe"),
                    Optional.of("Test Bank"),
                    Optional.of("123456"),
                    Optional.of("987654321"),
                    Optional.of("Checking"),
                    Optional.of("TID12345"),
                    Optional.of("BID12345"),
                    Optional.of("NAI12345")));

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
        var result = bisq.account.accounts.SameBankAccount.fromProto(PROTO);
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("creationDate")
                .isEqualTo(ACCOUNT);
        assertThat(result.getCreationDate()).isCloseTo(currentTimeMillis(), offset(1000L));
    }
}