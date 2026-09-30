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

package bisq.network.p2p.services.data.storage.auth;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The metaData field of the remove requests is annotated with @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3}).
 * Proto uses getVersion() to decide about the exclusion. serializeForHash() is used for the proof of work payload,
 * so it must stay identical across node versions.
 */
class DataRequestVersionTest {
    private static final ByteString HASH = ByteString.copyFrom(new byte[20]);
    private static final ByteString SIGNATURE = ByteString.copyFrom(new byte[70]);
    private static final ByteString PUBLIC_KEY = ByteString.copyFrom(KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded());
    private static final bisq.network.protobuf.MetaData META_DATA =
            new MetaData(MetaData.TTL_10_DAYS, "TestDistributedData").toProto(false);

    @Test
    void removeAuthenticatedDataRequestExcludesMetaDataFromHashInVersion1()
            throws InvalidProtocolBufferException, UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.fromProto(
                bisq.network.protobuf.RemoveAuthenticatedDataRequest.newBuilder()
                        .setVersion(1)
                        .setMetaData(META_DATA)
                        .setHash(HASH)
                        .setOwnerPublicKeyBytes(PUBLIC_KEY)
                        .setSequenceNumber(1)
                        .setSignature(SIGNATURE)
                        .setCreated(System.currentTimeMillis())
                        .build());

        assertEquals(1, request.getVersion());
        assertFalse(parse(request.serializeForHash()).getRemoveAuthenticatedDataRequest().hasMetaData());
        assertTrue(parse(request.serialize()).getRemoveAuthenticatedDataRequest().hasMetaData());
    }

    @Test
    void removeMailboxRequestExcludesMetaDataFromHashInVersion1()
            throws InvalidProtocolBufferException, UnresolvableProtobufException {
        RemoveMailboxRequest request = RemoveMailboxRequest.fromProto(
                bisq.network.protobuf.RemoveMailboxRequest.newBuilder()
                        .setVersion(1)
                        .setMetaData(META_DATA)
                        .setHash(HASH)
                        .setReceiverPublicKeyBytes(PUBLIC_KEY)
                        .setSignature(SIGNATURE)
                        .setCreated(System.currentTimeMillis())
                        .build());

        assertEquals(1, request.getVersion());
        assertFalse(parse(request.serializeForHash()).getRemoveMailboxRequest().hasMetaData());
        assertTrue(parse(request.serialize()).getRemoveMailboxRequest().hasMetaData());
    }

    @Test
    void removeAuthenticatedDataRequestExcludesClassNameFromHash()
            throws InvalidProtocolBufferException, UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.fromProto(
                removeAuthenticatedDataRequestProto().setClassName("TestDistributedData").build());

        assertEquals("TestDistributedData", request.getClassName());
        assertEquals("", parse(request.serializeForHash()).getRemoveAuthenticatedDataRequest().getClassName());
        assertEquals("TestDistributedData", parse(request.serialize()).getRemoveAuthenticatedDataRequest().getClassName());
    }

    @Test
    void removeAuthenticatedDataRequestWithoutClassNameUsesClassNameOfMetaData() throws UnresolvableProtobufException {
        // Nodes of earlier versions send no className
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.fromProto(
                removeAuthenticatedDataRequestProto().build());

        assertEquals("TestDistributedData", request.getClassName());
    }

    @Test
    void refreshAuthenticatedDataRequestReportsVersion() throws UnresolvableProtobufException {
        RefreshAuthenticatedDataRequest request = RefreshAuthenticatedDataRequest.fromProto(
                refreshAuthenticatedDataRequestProto().build());

        assertEquals(1, request.getVersion());
    }

    @Test
    void refreshAuthenticatedDataRequestExcludesClassNameFromHash()
            throws InvalidProtocolBufferException, UnresolvableProtobufException {
        RefreshAuthenticatedDataRequest request = RefreshAuthenticatedDataRequest.fromProto(
                refreshAuthenticatedDataRequestProto().setClassName("TestDistributedData").build());

        assertEquals("TestDistributedData", request.getClassName());
        assertEquals("", parse(request.serializeForHash()).getRefreshAuthenticatedDataRequest().getClassName());
        assertEquals("TestDistributedData",
                parse(request.serialize()).getRefreshAuthenticatedDataRequest().getClassName());
    }

    @Test
    void refreshAuthenticatedDataRequestWithoutClassNameUsesClassNameOfMetaData() throws UnresolvableProtobufException {
        // Nodes of earlier versions send no className
        RefreshAuthenticatedDataRequest request = RefreshAuthenticatedDataRequest.fromProto(
                refreshAuthenticatedDataRequestProto().build());

        assertEquals("TestDistributedData", request.getClassName());
    }

    private static bisq.network.protobuf.RefreshAuthenticatedDataRequest.Builder refreshAuthenticatedDataRequestProto() {
        //noinspection deprecation
        return bisq.network.protobuf.RefreshAuthenticatedDataRequest.newBuilder()
                .setVersion(1)
                .setMetaData(META_DATA)
                .setHash(HASH)
                .setOwnerPublicKeyBytes(PUBLIC_KEY)
                .setSequenceNumber(2)
                .setSignature(SIGNATURE)
                .setCreated(System.currentTimeMillis());
    }

    private static bisq.network.protobuf.RemoveAuthenticatedDataRequest.Builder removeAuthenticatedDataRequestProto() {
        //noinspection deprecation
        return bisq.network.protobuf.RemoveAuthenticatedDataRequest.newBuilder()
                .setVersion(1)
                .setMetaData(META_DATA)
                .setHash(HASH)
                .setOwnerPublicKeyBytes(PUBLIC_KEY)
                .setSequenceNumber(1)
                .setSignature(SIGNATURE)
                .setCreated(System.currentTimeMillis());
    }

    private static bisq.network.protobuf.DataRequest parse(byte[] envelopePayloadMessage) throws InvalidProtocolBufferException {
        return bisq.network.protobuf.EnvelopePayloadMessage.parseFrom(envelopePayloadMessage).getDataRequest();
    }
}
