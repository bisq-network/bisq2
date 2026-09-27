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

package bisq.network.p2p.services.data.storage;

import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.proto.ProtobufUtils;
import bisq.common.util.StringUtils;
import com.google.protobuf.Any;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The google.protobuf.Any in which the network classes hold DistributedData of higher-level modules
 * (AuthenticatedData.distributedData, AddAppendOnlyDataRequest.appendOnlyData).
 * <p>
 * A network class keeps the Any of received DistributedData and writes it unchanged when the data is relayed,
 * persisted, or sent in an inventory response. The DistributedData object is decoded from it for use, but it is never
 * used to rebuild the Any. Fields which this node does not know, for example fields which a newer version added, are
 * therefore not lost. See docs/specifications/network/distributed-data-payload-bytes.md.
 */
public final class DistributedDataAny {
    public static final int MAX_EXTRA_PAYLOAD_BYTES = 10 * 1024;
    private static final String TYPE_URL_PREFIX = "type.googleapis.com/";

    /**
     * Canonical encoding of a retained Any (google/protobuf/any.proto: string type_url = 1; bytes value = 2). The value
     * bytes are written unchanged. For an Any returned by {@link #retain(Any)} or created by {@link Any#pack}, the
     * bytes are identical to the protobuf serialization of the Any.
     */
    public static final CanonicalSchema<Any> SCHEMA = CanonicalSchema.<Any>newBuilder()
            .string(1, Any::getTypeUrl)
            .bytes(2, any -> any.getValue().toByteArray())
            .build();

    /**
     * Returns the Any which a network class keeps for received DistributedData: the type URL and the value bytes of
     * the received Any. The value bytes are not decoded and encoded again. Unknown fields of the Any message itself are
     * not kept; google.protobuf.Any has no other fields.
     * <p>
     * The type URL must be the one that {@link Any#pack} creates, which all Bisq versions use. The type URL is part of
     * the retained bytes. Accepting other type URLs which resolve to the same type would let a relay change these
     * bytes without changing the decoded data.
     * <p>
     * The returned Any is a new instance. Decode the DistributedData from the received Any, not from the returned one:
     * {@link Any#unpack} caches the decoded protobuf message in the Any instance, and the retained instance would keep
     * that message in memory. The holder must call {@link #validateSize(Any, DistributedData)} with the decoded data
     * before accepting the retained Any.
     *
     * @throws IllegalArgumentException if the type URL is not the one that {@link Any#pack} creates for the type
     */
    public static Any retain(Any receivedAny) {
        String typeUrl = receivedAny.getTypeUrl();
        checkArgument(typeUrl.equals(TYPE_URL_PREFIX + ProtobufUtils.getProtoType(receivedAny)),
                "Invalid type URL of distributed data: %s", StringUtils.truncate(typeUrl, 200));
        return Any.newBuilder()
                .setTypeUrl(typeUrl)
                .setValue(receivedAny.getValue())
                .build();
    }

    /**
     * Bounds the extra bytes that a holder retains beyond its decoded model. Checking only a protobuf
     * UnknownFieldSet would miss nested unknown fields and redundant encodings of known fields that decoding drops.
     * The complete serialization is the reference: known fields excluded from hashes still belong to the payload.
     */
    public static void validateSize(Any retainedAny, DistributedData distributedData) {
        int extraBytes = retainedAny.getValue().size() - distributedData.toProto(false).getSerializedSize();
        checkArgument(extraBytes <= MAX_EXTRA_PAYLOAD_BYTES,
                "Distributed data exceeds the extra payload byte limit: %s > %s", extraBytes, MAX_EXTRA_PAYLOAD_BYTES);
    }

    private DistributedDataAny() {
    }
}
