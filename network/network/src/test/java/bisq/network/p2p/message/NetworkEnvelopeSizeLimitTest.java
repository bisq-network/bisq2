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

package bisq.network.p2p.message;

import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.AuthorizationTokenType;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.data.inventory.InventoryRequest;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilter;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilterEntry;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.UnknownFieldSet;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// See rule 6 of docs/specifications/network/message-framing.md.
class NetworkEnvelopeSizeLimitTest {
    @Test
    void largestInventoryRequestIsBelowTheSizeLimit() {
        // The largest hash set filter: the most entries, and sequence numbers with the longest encoding.
        List<HashSetFilterEntry> entries = new ArrayList<>(HashSetFilter.MAX_ENTRIES);
        for (int i = 0; i < HashSetFilter.MAX_ENTRIES; i++) {
            byte[] hash = ByteBuffer.allocate(20).putInt(i).array();
            entries.add(new HashSetFilterEntry(hash, Integer.MIN_VALUE));
        }
        InventoryRequest inventoryRequest = new InventoryRequest(
                new HashSetFilter(InventoryFilterType.HASH_SET_V2, entries), Integer.MIN_VALUE);

        AuthorizationService authorizationService = new AuthorizationService(
                new AuthorizationService.Config(List.of(AuthorizationTokenType.HASH_CASH_V2)),
                new HashCashProofOfWorkService(),
                new EquihashProofOfWorkService(),
                Set.of(Feature.AUTHORIZATION_HASH_CASH_V2));
        AuthorizationToken token = authorizationService.createToken(inventoryRequest,
                new NetworkLoad(),
                "127.0.0.1:1234",
                Integer.MAX_VALUE,
                List.of(Feature.AUTHORIZATION_HASH_CASH_V2));
        NetworkEnvelope networkEnvelope = new NetworkEnvelope(Integer.MIN_VALUE, token, inventoryRequest);

        assertThat(networkEnvelope.completeProto().getSerializedSize())
                .isLessThan(NetworkEnvelope.MAX_SERIALIZED_SIZE);
    }

    // The legacy HASH_CASH token contains a copy of the message, so the envelope is about twice as large as the
    // message. Nodes use it for peers before version 2.1.3. The responder accepts requests of up to the default
    // inventory size of 2 000 000 bytes. Small entries need more bytes for the repeated field, so the test uses entries
    // of 100 bytes, below the size of a signed storage request.
    @Test
    void inventoryResponseOfTheDefaultSizeWithTheLegacyTokenIsBelowTheSizeLimit() {
        int entrySize = 100;
        bisq.network.protobuf.Inventory.Builder inventory = bisq.network.protobuf.Inventory.newBuilder()
                .setMaxSizeReached(true);
        for (int i = 0; i < 2_000_000 / entrySize; i++) {
            inventory.addEntries(dataRequestOfSize(entrySize));
        }
        bisq.network.protobuf.EnvelopePayloadMessage message = bisq.network.protobuf.EnvelopePayloadMessage.newBuilder()
                .setInventoryResponse(bisq.network.protobuf.InventoryResponse.newBuilder()
                        .setInventory(inventory)
                        .setRequestNonce(Integer.MIN_VALUE)
                        .setVersion(Integer.MIN_VALUE))
                .build();
        bisq.security.protobuf.ProofOfWork proofOfWork = bisq.security.protobuf.ProofOfWork.newBuilder()
                .setPayload(message.toByteString())
                .setCounter(Long.MIN_VALUE)
                .setChallenge(ByteString.copyFrom(new byte[32]))
                .setDifficulty(Double.MAX_VALUE)
                .setSolution(ByteString.copyFrom(new byte[8]))
                .setDuration(Long.MIN_VALUE)
                .build();
        bisq.network.protobuf.NetworkEnvelope envelope = bisq.network.protobuf.NetworkEnvelope.newBuilder()
                .setVersion(Integer.MIN_VALUE)
                .setAuthorizationToken(bisq.network.protobuf.AuthorizationToken.newBuilder()
                        .setAuthorizationTokenType(
                                bisq.network.protobuf.AuthorizationTokenType.AUTHORIZATIONTOKENTYPE_HASH_CASH)
                        .setHashCashToken(bisq.network.protobuf.HashCashToken.newBuilder()
                                .setProofOfWork(proofOfWork)
                                .setMessageCounter(Integer.MIN_VALUE)))
                .setNetworkMessage(message)
                .build();

        assertThat(envelope.getSerializedSize()).isLessThan(NetworkEnvelope.MAX_SERIALIZED_SIZE);
    }

    private static bisq.network.protobuf.DataRequest dataRequestOfSize(int size) {
        int fieldNumber = 1000;
        int paddingSize = size - CodedOutputStream.computeTagSize(fieldNumber) -
                CodedOutputStream.computeUInt32SizeNoTag(size);
        UnknownFieldSet padding = UnknownFieldSet.newBuilder()
                .addField(fieldNumber, UnknownFieldSet.Field.newBuilder()
                        .addLengthDelimited(ByteString.copyFrom(new byte[paddingSize]))
                        .build())
                .build();
        bisq.network.protobuf.DataRequest dataRequest = bisq.network.protobuf.DataRequest.newBuilder()
                .setUnknownFields(padding)
                .build();
        assertThat(dataRequest.getSerializedSize()).isEqualTo(size);
        return dataRequest;
    }
}
