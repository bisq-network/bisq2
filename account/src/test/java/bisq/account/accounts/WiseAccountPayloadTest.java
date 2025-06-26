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
import bisq.account.protobuf.CountryBasedAccountPayload;
import bisq.account.protobuf.WiseAccountPayload;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WiseAccountPayloadTest {

    private static final AccountPayload PROTO = AccountPayload.newBuilder()
            .setId("id")
            .setPaymentRailName("WISE")
            .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                    .setCountryCode("GB")
                    .setWiseAccountPayload(WiseAccountPayload.newBuilder()
                            .setEmail("test@example.com")
                            .setHolderName("John Doe")
                            .setBeneficiaryAddress("123 Main St, London")))
            .build();

    private static final AccountPayload PROTO_OPTIONAL_FIELDS_NOT_SET = AccountPayload.newBuilder()
            .setId("id")
            .setPaymentRailName("WISE")
            .setCountryBasedAccountPayload(CountryBasedAccountPayload.newBuilder()
                    .setCountryCode("GB")
                    .setWiseAccountPayload(WiseAccountPayload.newBuilder()
                            .setEmail("test@example.com")))
            .build();

    private static final bisq.account.accounts.WiseAccountPayload PAYLOAD =
            new bisq.account.accounts.WiseAccountPayload(
                    "id",
                    "WISE",
                    "GB",
                    "test@example.com",
                    Optional.of("John Doe"),
                    Optional.of("123 Main St, London"));

    private static final bisq.account.accounts.WiseAccountPayload PAYLOAD_OPTIONAL_FIELDS_NOT_SET =
            new bisq.account.accounts.WiseAccountPayload(
                    "id",
                    "WISE",
                    "GB",
                    "test@example.com",
                    Optional.empty(),
                    Optional.empty());

    @Test
    void testToProto() {
        assertEquals(PROTO, PAYLOAD.completeProto());
    }

    @Test
    void testFromProto() {
        assertEquals(PAYLOAD, bisq.account.accounts.WiseAccountPayload.fromProto(PROTO));
    }

    @Test
    void testToProtoWithOptionalFieldsNotSet() {
        assertEquals(PROTO_OPTIONAL_FIELDS_NOT_SET, PAYLOAD_OPTIONAL_FIELDS_NOT_SET.completeProto());
    }

    @Test
    void testFromProtoWithOptionalFieldsNotSet() {
        assertEquals(PAYLOAD_OPTIONAL_FIELDS_NOT_SET,
                bisq.account.accounts.WiseAccountPayload.fromProto(PROTO_OPTIONAL_FIELDS_NOT_SET));
    }

    @Test
    void testInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.WiseAccountPayload(
                        "id",
                        "WISE",
                        "GB",
                        "invalid-email",
                        Optional.empty(),
                        Optional.empty()));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.WiseAccountPayload(
                        "id",
                        "WISE",
                        "GB",
                        "a".repeat(90) + "@example.com", //too long email
                        Optional.empty(),
                        Optional.empty()));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.WiseAccountPayload(
                        "id",
                        "WISE",
                        "GB",
                        "", //empty email
                        Optional.empty(),
                        Optional.empty()));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.WiseAccountPayload(
                        "id",
                        "WISE",
                        "GB",
                        null, //null email
                        Optional.empty(),
                        Optional.empty()));
    }

    @Test
    void testHolderNameAndBeneficiaryAddressMustBeConsistent() {
        // Either both should be present or both should be absent
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.WiseAccountPayload(
                        "id",
                        "WISE",
                        "GB",
                        "test@example.com",
                        Optional.of("John Doe"),
                        Optional.empty()));

        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.WiseAccountPayload(
                        "id",
                        "WISE",
                        "GB",
                        "test@example.com",
                        Optional.empty(),
                        Optional.of("123 Main St, London")));

        // Both fields present (should not throw)
        assertDoesNotThrow(() ->
                new bisq.account.accounts.WiseAccountPayload(
                        "id",
                        "WISE",
                        "GB",
                        "test@example.com",
                        Optional.of("John Doe"),
                        Optional.of("123 Main St, London")));

        // Both fields absent (should not throw)
        assertDoesNotThrow(() ->
                new bisq.account.accounts.WiseAccountPayload(
                        "id",
                        "WISE",
                        "GB",
                        "test@example.com",
                        Optional.empty(),
                        Optional.empty()));
    }

    @Test
    void testHolderNameTooLong() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.WiseAccountPayload(
                        "id",
                        "WISE",
                        "GB",
                        "test@example.com",
                        Optional.of("A".repeat(101)),
                        Optional.of("123 Main St, London")));
    }

    @Test
    void testBeneficiaryAddressTooLong() {
        assertThrows(IllegalArgumentException.class, () ->
                new bisq.account.accounts.WiseAccountPayload(
                        "id",
                        "WISE",
                        "GB",
                        "test@example.com",
                        Optional.of("John Doe"),
                        Optional.of("A".repeat(201))));
    }

    @Test
    void testEquals() {
        bisq.account.accounts.WiseAccountPayload payload1 = new bisq.account.accounts.WiseAccountPayload(
                "id",
                "WISE",
                "GB",
                "test@example.com",
                Optional.of("John Doe"),
                Optional.of("123 Main St, London"));

        bisq.account.accounts.WiseAccountPayload payload2 = new bisq.account.accounts.WiseAccountPayload(
                "id",
                "WISE",
                "GB",
                "test@example.com",
                Optional.of("John Doe"),
                Optional.of("123 Main St, London"));

        assertEquals(payload1, payload2);
        assertEquals(payload1.hashCode(), payload2.hashCode());

        bisq.account.accounts.WiseAccountPayload differentPayload = new bisq.account.accounts.WiseAccountPayload(
                "id",
                "WISE",
                "GB",
                "different@example.com",
                Optional.of("John Doe"),
                Optional.of("123 Main St, London"));

        assertNotEquals(payload1, differentPayload);
    }
}