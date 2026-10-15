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

package bisq.network.p2p.services.data.storage.append;

import bisq.common.data.ByteArray;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.DistributedDataAny;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorageKeyV2;
import bisq.network.protobuf.DataRequest;
import bisq.security.DigestUtil;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class AddAppendOnlyDataRequest implements AddDataRequest {
    private final AppendOnlyData appendOnlyData;
    // The Any in which appendOnlyData is transported: as received, or as serialized once when this node created the
    // data. serialize() writes it unchanged, so that fields of the AppendOnlyData which this node does not know are
    // relayed, persisted, and sent in inventory responses (see DistributedDataAny).
    @Getter(AccessLevel.NONE)
    @ToString.Exclude
    private final Any appendOnlyDataAny;

    // For data created by this node
    public AddAppendOnlyDataRequest(AppendOnlyData appendOnlyData) {
        this(appendOnlyData, appendOnlyData.toAny(false));
    }

    // For received data: appendOnlyData must be decoded from the type URL and value of appendOnlyDataAny, which
    // DistributedDataAny.retain() returns.
    private AddAppendOnlyDataRequest(AppendOnlyData appendOnlyData, Any appendOnlyDataAny) {
        DistributedDataAny.validateSize(appendOnlyDataAny, appendOnlyData);
        this.appendOnlyData = appendOnlyData;
        this.appendOnlyDataAny = appendOnlyDataAny;

        verify();
    }

    @Override
    public void verify() {
    }


    /* --------------------------------------------------------------------- */
    // Protobuf
    /* --------------------------------------------------------------------- */

    @Override
    public DataRequest.Builder getDataRequestBuilder(boolean serializeForHash) {
        return newDataRequestBuilder().setAddAppendOnlyDataRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.AddAppendOnlyDataRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    // The hash serialization rebuilds the Any from appendOnlyData without the fields annotated with @ExcludeForHash, as
    // nodes of all versions compute it for the storage key, the proof of work, and the inventory order. A field which
    // this node does not know is not part of it.
    @Override
    public bisq.network.protobuf.AddAppendOnlyDataRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.AddAppendOnlyDataRequest.newBuilder()
                .setAppendOnlyData(serializeForHash ? appendOnlyData.toAny(true) : appendOnlyDataAny);
    }

    public static AddAppendOnlyDataRequest fromProto(bisq.network.protobuf.AddAppendOnlyDataRequest proto)
            throws UnresolvableProtobufException {
        try {
            Any appendOnlyDataAny = DistributedDataAny.retain(proto.getAppendOnlyData());
            return new AddAppendOnlyDataRequest((AppendOnlyData) DistributedData.fromAny(proto.getAppendOnlyData()),
                    appendOnlyDataAny);
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    // The retained Any, with the type URL and the exact bytes of the AppendOnlyData that serialize() writes. It
    // contains the fields annotated with @ExcludeForHash and the fields which this node does not know. The SIGNATURE
    // purpose uses the default schema.
    public static final CanonicalSchema<AddAppendOnlyDataRequest> DEFAULT_SCHEMA =
            bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(6,
                    CanonicalSchema.<AddAppendOnlyDataRequest>newBuilder()
                            .compose(1, request -> request.appendOnlyDataAny, DistributedDataAny.SCHEMA));

    // PROOF_OF_WORK and TRADE_CONTRACT reproduce serializeForHash(): nodes of all versions must compute the same proof
    // of work payload and contract hash. They differ from DEFAULT_SCHEMA only in the value of the Any: the encoding of
    // the decoded AppendOnlyData for the same purpose instead of the kept bytes. It equals the Any of toAny(true), which
    // getValueBuilder(true) writes.
    public static final CanonicalSchema<AddAppendOnlyDataRequest> PROOF_OF_WORK_SCHEMA =
            createSchemaForHash(EncodingPurpose.PROOF_OF_WORK);
    public static final CanonicalSchema<AddAppendOnlyDataRequest> TRADE_CONTRACT_SCHEMA =
            createSchemaForHash(EncodingPurpose.TRADE_CONTRACT);

    private static CanonicalSchema<AddAppendOnlyDataRequest> createSchemaForHash(EncodingPurpose encodingPurpose) {
        return bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(6,
                CanonicalSchema.<AddAppendOnlyDataRequest>newBuilder()
                        .compose(1, request -> request.getAppendOnlyDataAnyForHash(encodingPurpose), DistributedDataAny.SCHEMA));
    }

    // The kept type URL is the one that Any.pack creates (DistributedDataAny.retain checks it)
    private Any getAppendOnlyDataAnyForHash(EncodingPurpose encodingPurpose) {
        return Any.newBuilder()
                .setTypeUrl(appendOnlyDataAny.getTypeUrl())
                .setValue(ByteString.copyFrom(appendOnlyData.encodeCanonical(encodingPurpose)))
                .build();
    }

    // The storage key does not use an encoding of the request (see getStorageKeyPreImageV2).
    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<AddAppendOnlyDataRequest> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            case STORAGE_KEY ->
                    throw new IllegalArgumentException("Storage key not supported for AddAppendOnlyDataRequest");
            case PROOF_OF_WORK -> PROOF_OF_WORK_SCHEMA;
            case TRADE_CONTRACT -> TRADE_CONTRACT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }


    /* --------------------------------------------------------------------- */
    // DataRequest implementation
    /* --------------------------------------------------------------------- */

    @Override
    public ByteArray getStorageKeyV1() {
        return new ByteArray(DigestUtil.hash(appendOnlyData.serializeForHash()));
    }

    // The data hash uses the STORAGE_KEY encoding of the decoded AppendOnlyData. It leaves out the fields annotated
    // with @ExcludeForHash and the fields which this node does not know, so that such fields cannot create several
    // storage keys for the same data. The kept Any bytes are therefore not used.
    @Override
    public byte[] getStorageKeyPreImageV2() {
        byte[] appendOnlyDataHash = DigestUtil.hash(appendOnlyData.encodeCanonical(EncodingPurpose.STORAGE_KEY));
        return StorageKeyV2.createPreImage(StorageKeyV2.APPEND_ONLY_DATA_TAG, appendOnlyDataHash);
    }


    /* --------------------------------------------------------------------- */
    // Expirable implementation
    /* --------------------------------------------------------------------- */

    @Override
    public long getCreated() {
        // Used for sorting at pruning, but AppendOnlyData does not get pruned
        return 0;
    }

    @Override
    public boolean isExpired() {
        // AppendOnlyData never expires
        return false;
    }


    /* --------------------------------------------------------------------- */
    // StorageConstraint
    /* --------------------------------------------------------------------- */

    @Override
    public MetaData getMetaData() {
        return appendOnlyData.getMetaData();
    }
}
