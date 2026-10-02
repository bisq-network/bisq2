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

import bisq.common.encoding.Hex;
import bisq.common.encoding.canonical.Canonical;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.DistributedData;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Data which ensures that the sequence of add or remove request is maintained correctly.
 * The data gets hashed and signed and need to be deterministic.
 */
@Slf4j
@Getter
public final class AuthenticatedSequentialData implements NetworkProto, Canonical {
    private final AuthenticatedData authenticatedData;
    private final int sequenceNumber;
    private final byte[] pubKeyHash;
    private final long created;

    public AuthenticatedSequentialData(AuthenticatedData authenticatedData,
                                       int sequenceNumber,
                                       byte[] pubKeyHash,
                                       long created) {
        this.authenticatedData = authenticatedData;
        this.sequenceNumber = sequenceNumber;
        this.pubKeyHash = pubKeyHash;
        this.created = created;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(sequenceNumber > 0,
                "sequenceNumber must be positive. sequenceNumber=" + sequenceNumber);
        NetworkDataValidation.validateHash(pubKeyHash);
        NetworkDataValidation.validateDate(created);
        checkArgument(!isOwnerInvalid(authenticatedData, pubKeyHash, created),
                "The publisher is not the owner of the data");
    }

    @Override
    public bisq.network.protobuf.AuthenticatedSequentialData toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.AuthenticatedSequentialData.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.AuthenticatedSequentialData.newBuilder()
                .setAuthenticatedData(authenticatedData.toProto(serializeForHash))
                .setSequenceNumber(sequenceNumber)
                .setPubKeyHash(ByteString.copyFrom(pubKeyHash))
                .setCreated(created);
    }

    public static AuthenticatedSequentialData fromProto(bisq.network.protobuf.AuthenticatedSequentialData proto)
            throws UnresolvableProtobufException {
        try {
            return new AuthenticatedSequentialData(AuthenticatedData.fromProto(proto.getAuthenticatedData()),
                    proto.getSequenceNumber(),
                    proto.getPubKeyHash().toByteArray(),
                    proto.getCreated());
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    public static final CanonicalSchema<AuthenticatedSequentialData> DEFAULT_SCHEMA =
            CanonicalSchema.<AuthenticatedSequentialData>newBuilder()
                    .canonical(1, AuthenticatedSequentialData::getAuthenticatedData)
                    .sint32(2, AuthenticatedSequentialData::getSequenceNumber)
                    .bytes(3, AuthenticatedSequentialData::getPubKeyHash)
                    .sint64(4, AuthenticatedSequentialData::getCreated)
                    .build();

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<AuthenticatedSequentialData> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            // Only the stored data has a storage key encoding (see docs/specifications/network/storage-keys.md)
            case STORAGE_KEY ->
                    throw new IllegalArgumentException("Storage key not supported for AuthenticatedSequentialData");
            case PROOF_OF_WORK -> DEFAULT_SCHEMA;
            case TRADE_CONTRACT -> DEFAULT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }

    public DistributedData getDistributedData() {
        return authenticatedData.getDistributedData();
    }

    // pubKeyHash is the hash of the key which signs the request. It must be the owner of the data, unless the transition
    // rule accepts another owner for the creation date (see docs/specifications/network/storage-ownership.md). The store
    // uses it for a refresh, which replaces the creation date of a stored entry.
    public static boolean isOwnerInvalid(AuthenticatedData authenticatedData, byte[] pubKeyHash, long created) {
        return !authenticatedData.isOwner(pubKeyHash) && !authenticatedData.isOtherOwnerAccepted(created);
    }

    @Override
    public String toString() {
        return "AuthenticatedSequentialData{" +
                "\r\n          sequenceNumber=" + sequenceNumber +
                ",\r\n          created=" + new Date(created) + " (" + created + ")" +
                ",\r\n          pubKeyHash=" + Hex.encode(pubKeyHash) +
                ",\r\n          authenticatedData=" + authenticatedData +
                "\r\n}";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthenticatedSequentialData that)) return false;

        return sequenceNumber == that.sequenceNumber &&
                created == that.created &&
                Objects.equals(authenticatedData, that.authenticatedData) &&
                Arrays.equals(pubKeyHash, that.pubKeyHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(authenticatedData);
        result = 31 * result + sequenceNumber;
        result = 31 * result + Long.hashCode(created);
        result = 31 * result + Arrays.hashCode(pubKeyHash);
        return result;
    }
}
