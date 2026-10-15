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
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

/**
 * The deprecated metaData field of the remove requests is excluded from the hash. Versions 1 to 3 are the versions
 * which supported nodes send; for them serializeForHash(), the proof of work payload, is the same as in nodes of
 * earlier versions. Version 0 fails at parsing. Before the cutoff date of signature V1, a remove request of a version
 * above 3 fails at parsing as well.
 */
class DataRequestVersionTest {
    private static final ByteString HASH = ByteString.copyFrom(new byte[20]);
    private static final ByteString SIGNATURE = ByteString.copyFrom(new byte[70]);
    private static final ByteString PUBLIC_KEY = ByteString.copyFrom(KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded());
    private static final bisq.network.protobuf.MetaData META_DATA =
            new MetaData(MetaData.TTL_10_DAYS, "TestDistributedData").toProto(false);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void removeAuthenticatedDataRequestExcludesMetaDataFromHash(int version)
            throws InvalidProtocolBufferException, UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.fromProto(
                bisq.network.protobuf.RemoveAuthenticatedDataRequest.newBuilder()
                        .setVersion(version)
                        .setMetaData(META_DATA)
                        .setHash(HASH)
                        .setOwnerPublicKeyBytes(PUBLIC_KEY)
                        .setSequenceNumber(1)
                        .setSignature(SIGNATURE)
                        .setCreated(System.currentTimeMillis())
                        .build());

        assertEquals(version, request.getVersion());
        assertFalse(parse(request.serializeForHash()).getRemoveAuthenticatedDataRequest().hasMetaData());
        assertTrue(parse(request.serialize()).getRemoveAuthenticatedDataRequest().hasMetaData());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void removeMailboxRequestExcludesMetaDataFromHash(int version)
            throws InvalidProtocolBufferException, UnresolvableProtobufException {
        RemoveMailboxRequest request = RemoveMailboxRequest.fromProto(
                removeMailboxRequestProto().setVersion(version).build());

        assertEquals(version, request.getVersion());
        assertFalse(parse(request.serializeForHash()).getRemoveMailboxRequest().hasMetaData());
        assertTrue(parse(request.serialize()).getRemoveMailboxRequest().hasMetaData());
    }

    // Version 0 is the format of releases before the version field existed. Since v2.1.0, nodes send version 1.
    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void requestsOfVersion0FailAtParsing(int version) {
        assertRequestsFailAtParsing(version);
    }

    // Nodes up to 2.1.13 include metaData in serializeForHash() of the remove requests in versions above 3. Any key
    // pair can sign such a request, also with signature V2, and signature V1 does not cover the version, so a relay
    // could also change it in a request of these nodes.
    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    void removeRequestsOfVersionAbove3FailAtParsingBeforeTheCutoffDate(int version) {
        try (MockedStatic<SignedRequest> signedRequest = mockStatic(SignedRequest.class, CALLS_REAL_METHODS)) {
            signedRequest.when(() -> SignedRequest.isSignatureV2Required(anyLong())).thenReturn(false);

            for (ByteString signatureV2 : List.of(ByteString.EMPTY, SIGNATURE)) {
                assertThrows(UnresolvableProtobufException.class, () -> RemoveAuthenticatedDataRequest.fromProto(
                        removeAuthenticatedDataRequestProto().setVersion(version).setSignatureV2(signatureV2).build()));
                assertThrows(UnresolvableProtobufException.class, () -> RemoveMailboxRequest.fromProto(
                        removeMailboxRequestProto().setVersion(version).setSignatureV2(signatureV2).build()));
            }
        }
    }

    // From the cutoff date on, nodes up to 2.1.13 cannot publish data anymore, and a later release can raise the
    // version
    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    void removeRequestsOfVersionAbove3ParseFromTheCutoffDate(int version)
            throws InvalidProtocolBufferException, UnresolvableProtobufException {
        try (MockedStatic<SignedRequest> signedRequest = mockStatic(SignedRequest.class, CALLS_REAL_METHODS)) {
            signedRequest.when(() -> SignedRequest.isSignatureV2Required(anyLong())).thenReturn(true);

            RemoveAuthenticatedDataRequest authenticatedDataRequest = RemoveAuthenticatedDataRequest.fromProto(
                    removeAuthenticatedDataRequestProto().setVersion(version).setSignatureV2(SIGNATURE).build());
            RemoveMailboxRequest mailboxRequest = RemoveMailboxRequest.fromProto(
                    removeMailboxRequestProto().setVersion(version).setSignatureV2(SIGNATURE).build());

            assertEquals(version, authenticatedDataRequest.getVersion());
            assertFalse(parse(authenticatedDataRequest.serializeForHash()).getRemoveAuthenticatedDataRequest()
                    .hasMetaData());
            assertEquals(version, mailboxRequest.getVersion());
            assertFalse(parse(mailboxRequest.serializeForHash()).getRemoveMailboxRequest().hasMetaData());
        }
    }

    // Nodes up to 2.1.13 leave metaData of the refresh request out of the hash in every version
    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    void refreshRequestOfVersionAbove3Parses(int version) throws UnresolvableProtobufException {
        RefreshAuthenticatedDataRequest request = RefreshAuthenticatedDataRequest.fromProto(
                refreshAuthenticatedDataRequestProto().setVersion(version).setSignatureV2(SIGNATURE).build());

        assertEquals(version, request.getVersion());
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

    private static void assertRequestsFailAtParsing(int version) {
        assertThrows(UnresolvableProtobufException.class, () -> RemoveAuthenticatedDataRequest.fromProto(
                removeAuthenticatedDataRequestProto().setVersion(version).build()));
        assertThrows(UnresolvableProtobufException.class, () -> RemoveMailboxRequest.fromProto(
                removeMailboxRequestProto().setVersion(version).build()));
        assertThrows(UnresolvableProtobufException.class, () -> RefreshAuthenticatedDataRequest.fromProto(
                refreshAuthenticatedDataRequestProto().setVersion(version).build()));
    }

    private static bisq.network.protobuf.RemoveMailboxRequest.Builder removeMailboxRequestProto() {
        //noinspection deprecation
        return bisq.network.protobuf.RemoveMailboxRequest.newBuilder()
                .setVersion(1)
                .setMetaData(META_DATA)
                .setHash(HASH)
                .setReceiverPublicKeyBytes(PUBLIC_KEY)
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
