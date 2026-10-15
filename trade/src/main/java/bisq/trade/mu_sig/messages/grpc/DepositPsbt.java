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

package bisq.trade.mu_sig.messages.grpc;

import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufException;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.util.Arrays;

@Getter
public final class DepositPsbt implements NetworkProto {
    private final byte[] depositPsbt;

    public DepositPsbt(byte[] depositPsbt) {
        this.depositPsbt = depositPsbt;
    }

    @Override
    public void verify() {
        //todo
    }

    @Override
    public bisq.trade.protobuf.DepositPsbt.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.DepositPsbt.newBuilder()
                .setDepositPsbt(ByteString.copyFrom(depositPsbt));
    }

    @Override
    public bisq.trade.protobuf.DepositPsbt toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static DepositPsbt fromProto(bisq.trade.protobuf.DepositPsbt proto) throws UnresolvableProtobufException {
        try {
            return new DepositPsbt(proto.getDepositPsbt().toByteArray());
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DepositPsbt that)) return false;

        return Arrays.equals(depositPsbt, that.depositPsbt);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(depositPsbt);
    }
}
