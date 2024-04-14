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

package bisq.network.p2p.services.data.inventory.filter.hash_set;

import bisq.common.encoding.Hex;
import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Arrays;

@Getter
@EqualsAndHashCode
public final class HashSetFilterEntry implements NetworkProto, Comparable<HashSetFilterEntry> {
    private final byte[] hash;
    private final int sequenceNumber;

    public HashSetFilterEntry(byte[] hash, int sequenceNumber) {
        this.hash = hash;
        this.sequenceNumber = sequenceNumber;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(hash);
    }

    @Override
    public bisq.network.protobuf.HashSetFilterEntry toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.HashSetFilterEntry.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.HashSetFilterEntry.newBuilder()
                .setHash(ByteString.copyFrom(hash))
                .setSequenceNumber(sequenceNumber);
    }

    public static HashSetFilterEntry fromProto(bisq.network.protobuf.HashSetFilterEntry proto) {
        return new HashSetFilterEntry(proto.getHash().toByteArray(), proto.getSequenceNumber());
    }

    @Override
    public int compareTo(@Nonnull HashSetFilterEntry o) {
        return Arrays.compare(hash, o.getHash());
    }

    @Override
    public String toString() {
        return "HashSetFilterEntry{" +
                "hash (as hex)=" + Hex.encode(hash) +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}