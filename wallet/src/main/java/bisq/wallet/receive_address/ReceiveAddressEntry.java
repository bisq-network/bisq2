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

package bisq.wallet.receive_address;

import bisq.common.proto.PersistableProto;
import bisq.common.util.OptionalUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public final class ReceiveAddressEntry implements PersistableProto {
    private final String address;
    private final long createdAt;
    private final Optional<String> note;

    public ReceiveAddressEntry(String address) {
        this(address,
                System.currentTimeMillis(),
                Optional.empty());
    }

    @Override
    public bisq.wallet.protobuf.ReceiveAddressEntry.Builder getBuilder(boolean serializeForHash) {
        bisq.wallet.protobuf.ReceiveAddressEntry.Builder builder = bisq.wallet.protobuf.ReceiveAddressEntry.newBuilder()
                .setAddress(address)
                .setCreatedAt(createdAt);
        note.ifPresent(builder::setNote);
        return builder;
    }

    @Override
    public bisq.wallet.protobuf.ReceiveAddressEntry toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static ReceiveAddressEntry fromProto(bisq.wallet.protobuf.ReceiveAddressEntry proto) {
        return new ReceiveAddressEntry(proto.getAddress(),
                proto.getCreatedAt(),
                OptionalUtils.optionalIf(proto.hasNote(), proto::getNote));
    }
}
