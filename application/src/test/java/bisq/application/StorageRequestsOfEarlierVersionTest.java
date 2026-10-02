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

package bisq.application;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.inventory.Inventory;
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parses storage requests which the code of the tag v2.1.13 created, so the test does not depend on how this version
 * creates requests. Nodes of version 2.1.13 accepted, stored and relayed all of them, and they serve them in inventory
 * responses.
 * <p>
 * A valid request of version 2.1.13 has signature V1 only. A node of this version parses it until the cutoff date of
 * signature V1. The other requests break integrity rules of this version, so parsing rejects them (rules 1 to 3 of
 * docs/specifications/network/validation-at-parsing.md). In an inventory response, the requester skips them and keeps
 * the other entries (rule 7 of docs/specifications/network/inventory-hash-set-filter.md).
 * <p>
 * Parsing compares the cutoff date with the time of the node, so the expected result of the valid request depends on
 * the date on which the test runs.
 */
class StorageRequestsOfEarlierVersionTest {
    private static Properties requests;
    private static int previousInventoryMaxSize;

    @BeforeAll
    static void setUp() throws IOException {
        ResolverConfig.config();
        previousInventoryMaxSize = Inventory.maxSize;
        Inventory.maxSize = 2_000_000;
        requests = new Properties();
        try (InputStream inputStream = StorageRequestsOfEarlierVersionTest.class.getResourceAsStream(
                "storage-requests-of-v2.1.13.properties")) {
            assertNotNull(inputStream);
            requests.load(inputStream);
        }
    }

    @AfterAll
    static void tearDown() {
        Inventory.maxSize = previousInventoryMaxSize;
    }

    @Test
    void validRequestIsParsedUntilTheCutoffDateOfSignatureV1() {
        if (isSignatureV2Required()) {
            assertThrows(UnresolvableProtobufException.class, () -> parse("poison.good.add"));
        } else {
            assertInstanceOf(AddAuthenticatedDataRequest.class, parseValidRequest("poison.good.add"));
        }
    }

    // The reason makes sure that the integrity rule rejects the request, and not another failure, for example a missing
    // resolver.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "poison.add.sequenceNumber0 | sequenceNumber must be positive",
            "poison.remove.sequenceNumber0 | sequenceNumber must be positive",
            "poison.remove.compressedKey | EC public key is not in canonical encoding",
            "poison.mailbox.sequenceNumber2 | sequenceNumber must be 1"
    })
    void requestWhichBreaksAnIntegrityRuleIsRejectedAtParsing(String name, String reason) {
        UnresolvableProtobufException exception = assertThrows(UnresolvableProtobufException.class,
                () -> parse(name));
        IllegalArgumentException cause = assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertTrue(cause.getMessage().startsWith(reason), cause.getMessage());
    }

    @Test
    void inventoryResponseOfANodeOfAnEarlierVersionKeepsTheEntriesWhichTheNodeCanParse() throws Exception {
        bisq.network.protobuf.Inventory proto = bisq.network.protobuf.Inventory.parseFrom(
                decode("poison.inventoryOfOldNode"));
        assertEquals(5, proto.getEntriesCount());

        List<? extends DataRequest> entries = Inventory.fromProto(proto).getEntries();

        if (isSignatureV2Required()) {
            assertEquals(List.of(), entries);
        } else {
            assertEquals(1, entries.size());
            assertEquals(parseValidRequest("poison.good.add"), entries.get(0));
        }
    }

    private static boolean isSignatureV2Required() {
        return SignedRequest.isSignatureV2Required(System.currentTimeMillis());
    }

    private static EnvelopePayloadMessage parseValidRequest(String name) {
        try {
            return parse(name);
        } catch (Exception e) {
            throw new AssertionError(name + " could not be parsed", e);
        }
    }

    private static EnvelopePayloadMessage parse(String name) throws Exception {
        return EnvelopePayloadMessage.fromProto(bisq.network.protobuf.EnvelopePayloadMessage.parseFrom(decode(name)));
    }

    private static byte[] decode(String name) {
        return Base64.getDecoder().decode(requests.getProperty(name));
    }
}
