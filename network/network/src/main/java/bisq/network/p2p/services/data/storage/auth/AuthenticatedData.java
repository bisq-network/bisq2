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

import bisq.common.encoding.canonical.Canonical;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.DistributedDataAny;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.OwnershipVerifiableData;
import bisq.network.p2p.services.data.storage.StorageData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import com.google.protobuf.Any;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * Base class for AuthenticatedData or AuthorizedData. Holds DistributedData.
 * We use Any for wrapping the external implementation of the distributedData instance (e.g. Offer).
 */
@Slf4j
@EqualsAndHashCode
public abstract class AuthenticatedData implements StorageData, Canonical {
    @Getter
    protected final DistributedData distributedData;
    // The Any in which distributedData is transported: as received, or as serialized once when this node created the
    // data. serialize() writes it unchanged, so that fields of the DistributedData which this node does not know are
    // relayed, persisted, and sent in inventory responses (see DistributedDataAny).
    protected final Any distributedDataAny;

    // For data created by this node
    public AuthenticatedData(DistributedData distributedData) {
        this(distributedData, distributedData.toAny(false));
    }

    // For received data: distributedData must be decoded from the type URL and value of distributedDataAny, which
    // DistributedDataAny.retain() returns.
    protected AuthenticatedData(DistributedData distributedData, Any distributedDataAny) {
        DistributedDataAny.validateSize(distributedDataAny, distributedData);
        this.distributedData = distributedData;
        this.distributedDataAny = distributedDataAny;
    }

    @Override
    public abstract bisq.network.protobuf.AuthenticatedData toProto(boolean serializeForHash);

    // The hash serialization rebuilds the Any from distributedData without the fields annotated with @ExcludeForHash,
    // as nodes of all versions compute it for storage keys, signatures, the proof of work, and the inventory order. A
    // field which this node does not know is not part of it.
    public bisq.network.protobuf.AuthenticatedData.Builder getAuthenticatedDataBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.AuthenticatedData.newBuilder()
                .setDistributedData(serializeForHash ? distributedData.toAny(true) : distributedDataAny);
    }

    public static AuthenticatedData fromProto(bisq.network.protobuf.AuthenticatedData proto) {
        return switch (proto.getMessageCase()) {
            case DEFAULTAUTHENTICATEDDATA -> DefaultAuthenticatedData.fromProto(proto);
            case AUTHORIZEDDATA -> AuthorizedData.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    // Used by the DEFAULT and SIGNATURE schemas. The retained Any, with the type URL and the exact bytes of the
    // DistributedData that serialize() writes. It contains the fields annotated with @ExcludeForHash and the fields
    // which this node does not know, so a signature over it covers every field of the DistributedData.
    protected static <T extends AuthenticatedData> CanonicalSchema.Builder<T> getAuthenticatedDataSchemaBuilder() {
        return CanonicalSchema.<T>newBuilder()
                .compose(1, authenticatedData -> authenticatedData.distributedDataAny, DistributedDataAny.SCHEMA);
    }

    // Used by the STORAGE_KEY, PROOF_OF_WORK and TRADE_CONTRACT schemas, which reproduce the hash serialization: nodes
    // of all versions must compute the same storage key, proof of work payload and contract hash. For AuthorizedData,
    // only STORAGE_KEY equals serializeForHash(); PROOF_OF_WORK and TRADE_CONTRACT equal toProto(true).
    protected static <T extends AuthenticatedData> CanonicalSchema.Builder<T> getSchemaBuilderForHash() {
        return CanonicalSchema.<T>newBuilder()
                // Canonical encoding is not yet supported for DistributedData, as its implementations are defined
                // outside the network module. We use the protobuf serialization for hashes instead: the Any with the
                // type URL and the serializeForHash() bytes, as getAuthenticatedDataBuilder(true) writes it.
                .encodedMessage(1, authenticatedData -> authenticatedData.distributedData.toAny(true).toByteArray());
    }

    // We delegate the delivery of MetaData to the distributedData.
    @Override
    public MetaData getMetaData() {
        return distributedData.getMetaData();
    }

    public String getClassName() {
        return distributedData.getClassName();
    }

    // Whether the key with this hash owns the data. Only the owner can add, refresh and remove the data in the
    // authenticated data store (see docs/specifications/network/storage-ownership.md). The owner is the one which the
    // distributed data names (OwnershipVerifiableData). Other data has no owner, so the store rejects it.
    public boolean isOwner(byte[] pubKeyHash) {
        return distributedData instanceof OwnershipVerifiableData ownershipVerifiableData &&
                Arrays.equals(ownershipVerifiableData.getOwnerPubKeyHash(), pubKeyHash);
    }

    // Whether data created at the given date can have another owner than isOwner() accepts. Only a transition rule of
    // authorized data allows it (see AuthorizedData).
    public boolean isOtherOwnerAccepted(long created) {
        return false;
    }

    @Override
    public String toString() {
        return "AuthenticatedData{" +
                "\r\n               distributedData=" + distributedData +
                "\r\n}";
    }
}
