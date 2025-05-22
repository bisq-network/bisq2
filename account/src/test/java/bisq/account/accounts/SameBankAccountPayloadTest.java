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

import bisq.account.protobuf.AccountPayload;
import bisq.account.protobuf.BankAccountPayload;
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.SameBankAccountPayload;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SameBankAccountPayloadTest {

    private static final AccountPayload PROTO_FULL = AccountPayload.newBuilder()
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
                            .setSameBankAccountPayload(SameBankAccountPayload.newBuilder())))
            .build();

    private static final AccountPayload PROTO_MINIMAL = AccountPayload.newBuilder()
            .setId("id")
            .setPaymentMethodName("NATIONAL_BANK")
            .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                    .setCountryCode("US")
                    .setBankAccountPayload(BankAccountPayload.newBuilder()
                            .setSameBankAccountPayload(SameBankAccountPayload.newBuilder())))
            .build();

    private static final bisq.account.accounts.SameBankAccountPayload PAYLOAD_FULL =
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
                    Optional.of("NAI12345"));

    private static final bisq.account.accounts.SameBankAccountPayload PAYLOAD_MINIMAL =
            new bisq.account.accounts.SameBankAccountPayload(
                    "id",
                    "NATIONAL_BANK",
                    "US",
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());

    @Test
    void testToProtoFull() {
        assertEquals(PROTO_FULL, PAYLOAD_FULL.completeProto());
    }

    @Test
    void testFromProtoFull() {
        assertEquals(PAYLOAD_FULL, bisq.account.accounts.SameBankAccountPayload.fromProto(PROTO_FULL));
    }

    @Test
    void testToProtoMinimal() {
        assertEquals(PROTO_MINIMAL, PAYLOAD_MINIMAL.completeProto());
    }

    @Test
    void testFromProtoMinimal() {
        assertEquals(PAYLOAD_MINIMAL, bisq.account.accounts.SameBankAccountPayload.fromProto(PROTO_MINIMAL));
    }

    @Test
    void testEquals() {
        bisq.account.accounts.SameBankAccountPayload payload1 = new bisq.account.accounts.SameBankAccountPayload(
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
                Optional.of("NAI12345"));

        bisq.account.accounts.SameBankAccountPayload payload2 = new bisq.account.accounts.SameBankAccountPayload(
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
                Optional.of("NAI12345"));

        assertEquals(payload1, payload2);
        assertEquals(payload1.hashCode(), payload2.hashCode());

        bisq.account.accounts.SameBankAccountPayload differentPayload = new bisq.account.accounts.SameBankAccountPayload(
                "id",
                "NATIONAL_BANK",
                "US",
                Optional.of("Jane Doe"),
                Optional.of("Test Bank"),
                Optional.of("123456"),
                Optional.of("987654321"),
                Optional.of("Checking"),
                Optional.of("TID12345"),
                Optional.of("BID12345"),
                Optional.of("NAI12345"));

        assertNotEquals(payload1, differentPayload);
    }

    @Test
    void testPartialOptionals() {
        bisq.account.accounts.SameBankAccountPayload partialPayload = new bisq.account.accounts.SameBankAccountPayload(
                "id",
                "NATIONAL_BANK",
                "US",
                Optional.of("John Doe"),
                Optional.of("Test Bank"),
                Optional.empty(),
                Optional.of("987654321"),
                Optional.empty(),
                Optional.empty(),
                Optional.of("BID12345"),
                Optional.empty());

        AccountPayload proto = partialPayload.completeProto();
        bisq.account.accounts.SameBankAccountPayload convertedPayload = bisq.account.accounts.SameBankAccountPayload.fromProto(proto);

        assertEquals(partialPayload, convertedPayload);

        assertEquals(Optional.of("John Doe"), convertedPayload.getHolderName());
        assertEquals(Optional.of("Test Bank"), convertedPayload.getBankName());
        assertEquals(Optional.empty(), convertedPayload.getBranchId());
        assertEquals(Optional.of("987654321"), convertedPayload.getAccountNr());
        assertEquals(Optional.empty(), convertedPayload.getAccountType());
        assertEquals(Optional.empty(), convertedPayload.getHolderTaxId());
        assertEquals(Optional.of("BID12345"), convertedPayload.getBankId());
        assertEquals(Optional.empty(), convertedPayload.getNationalAccountId());
    }

    @Test
    void testFieldValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SameBankAccountPayload(
                        "id",
                        "NATIONAL_BANK",
                        "US",
                        Optional.of("A".repeat(101)), // holder name > 100 chars
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SameBankAccountPayload(
                        "id",
                        "NATIONAL_BANK",
                        "US",
                        Optional.empty(),
                        Optional.of("A".repeat(101)), // bank name > 100 chars
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SameBankAccountPayload(
                        "id",
                        "NATIONAL_BANK",
                        "US",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("A".repeat(31)), // branch id > 30 chars
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SameBankAccountPayload(
                        "id",
                        "NATIONAL_BANK",
                        "US",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("A".repeat(31)), // account number > 30 chars
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SameBankAccountPayload(
                        "id",
                        "NATIONAL_BANK",
                        "US",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("A".repeat(21)), // account type > 20 chars
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SameBankAccountPayload(
                        "id",
                        "NATIONAL_BANK",
                        "US",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("A".repeat(51)), // holder tax id > 50 chars
                        Optional.empty(),
                        Optional.empty()));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SameBankAccountPayload(
                        "id",
                        "NATIONAL_BANK",
                        "US",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("A".repeat(51)), // bank id > 50 chars
                        Optional.empty()));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.SameBankAccountPayload(
                        "id",
                        "NATIONAL_BANK",
                        "US",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("A".repeat(51)))); // national account id > 50 chars
    }
}