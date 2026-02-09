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

package bisq.oracle_node.bisq1_bridge.grpc.messages;

import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class AccountTimestampRequest implements NetworkProto {
    private final byte[] hash;

    public AccountTimestampRequest(byte[] hash) {
        this.hash = hash;
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(hash);
    }

    @Override
    public bisq.bridge.protobuf.AccountTimestampDateRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.AccountTimestampDateRequest.newBuilder()
                .setHash(ByteString.copyFrom(hash));
    }

    @Override
    public bisq.bridge.protobuf.AccountTimestampDateRequest toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.bridge.protobuf.AccountTimestampDateRequest completeProto() {
        return toProto(false);
    }

    public static AccountTimestampRequest fromProto(bisq.bridge.protobuf.AccountTimestampDateRequest proto) {
        return new AccountTimestampRequest(proto.getHash().toByteArray());
    }
}
