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

import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.DistributedDataAny;
import com.google.protobuf.Any;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class DefaultAuthenticatedData extends AuthenticatedData {
    public DefaultAuthenticatedData(DistributedData distributedData) {
        super(distributedData);

        verify();
    }

    private DefaultAuthenticatedData(DistributedData distributedData, Any distributedDataAny) {
        super(distributedData, distributedDataAny);

        verify();
    }

    @Override
    public void verify() {
    }

    public bisq.network.protobuf.AuthenticatedData toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.AuthenticatedData.Builder getBuilder(boolean serializeForHash) {
        return getAuthenticatedDataBuilder(serializeForHash).setDefaultAuthenticatedData(
                bisq.network.protobuf.DefaultAuthenticatedData.newBuilder());
    }

    public static DefaultAuthenticatedData fromProto(bisq.network.protobuf.AuthenticatedData proto) {
        Any distributedDataAny = DistributedDataAny.retain(proto.getDistributedData());
        return new DefaultAuthenticatedData(DistributedData.fromAny(proto.getDistributedData()), distributedDataAny);
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    // The SIGNATURE purpose uses the default schema: it contains the DistributedData with its exact bytes.
    public static final CanonicalSchema<DefaultAuthenticatedData> DEFAULT_SCHEMA =
            DefaultAuthenticatedData.<DefaultAuthenticatedData>getAuthenticatedDataSchemaBuilder()
                    .extend(10,
                            defaultAuthenticatedData -> defaultAuthenticatedData,
                            CanonicalSchema.newBuilder())
                    .build();

    // STORAGE_KEY, PROOF_OF_WORK and TRADE_CONTRACT: same as DEFAULT_SCHEMA, but with the DistributedData in its hash
    // serialization, as serializeForHash() writes it
    public static final CanonicalSchema<DefaultAuthenticatedData> STORAGE_KEY_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<DefaultAuthenticatedData> PROOF_OF_WORK_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<DefaultAuthenticatedData> TRADE_CONTRACT_SCHEMA = createSchemaForHash();

    private static CanonicalSchema<DefaultAuthenticatedData> createSchemaForHash() {
        return DefaultAuthenticatedData.<DefaultAuthenticatedData>getSchemaBuilderForHash()
                .extend(10,
                        defaultAuthenticatedData -> defaultAuthenticatedData,
                        CanonicalSchema.newBuilder())
                .build();
    }

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<DefaultAuthenticatedData> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            case STORAGE_KEY -> STORAGE_KEY_SCHEMA;
            case PROOF_OF_WORK -> PROOF_OF_WORK_SCHEMA;
            case TRADE_CONTRACT -> TRADE_CONTRACT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }

    @Override
    public boolean isDataInvalid() {
        return distributedData.isDataInvalid();
    }
}
