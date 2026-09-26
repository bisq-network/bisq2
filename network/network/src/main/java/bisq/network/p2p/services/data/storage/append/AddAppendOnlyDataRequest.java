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
import bisq.common.util.MathUtils;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.protobuf.DataRequest;
import bisq.security.DigestUtil;
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

    public AddAppendOnlyDataRequest(AppendOnlyData appendOnlyData) {
        this.appendOnlyData = appendOnlyData;

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

    @Override
    public bisq.network.protobuf.AddAppendOnlyDataRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.AddAppendOnlyDataRequest.newBuilder()
                .setAppendOnlyData(appendOnlyData.toAny(serializeForHash));
    }

    public static AddAppendOnlyDataRequest fromProto(bisq.network.protobuf.AddAppendOnlyDataRequest proto) {
        return new AddAppendOnlyDataRequest((AppendOnlyData) DistributedData.fromAny(proto.getAppendOnlyData()));
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    public static final CanonicalSchema<AddAppendOnlyDataRequest> DEFAULT_SCHEMA =
            bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(6,
                    CanonicalSchema.<AddAppendOnlyDataRequest>newBuilder()
                            // Canonical encoding is not yet supported for AppendOnlyData, as its implementations are
                            // defined outside the network module. We use the protobuf serialization for hashes
                            // instead: the Any with the type URL and the serializeForHash() bytes, as
                            // getValueBuilder(true) writes it.
                            .encodedMessage(1, request -> request.appendOnlyData.toAny(true).toByteArray()));

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<AddAppendOnlyDataRequest> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            case STORAGE_KEY -> DEFAULT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }


    /* --------------------------------------------------------------------- */
    // CostBearingMessage implementation
    /* --------------------------------------------------------------------- */

    @Override
    public double getCostFactor() {
        return MathUtils.bounded(0.5, 1, appendOnlyData.getCostFactor());
    }


    /* --------------------------------------------------------------------- */
    // DataRequest implementation
    /* --------------------------------------------------------------------- */

    @Override
    public ByteArray getStorageKey() {
        byte[] hash = DigestUtil.hash(getStoragePreImage());
        return new ByteArray(hash);
    }

    private byte[] getStoragePreImage() {
        return appendOnlyData.serializeForHash();
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
