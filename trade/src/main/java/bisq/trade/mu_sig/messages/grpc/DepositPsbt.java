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

import bisq.common.proto.Proto;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class DepositPsbt implements Proto {
    private final byte[] depositPsbt;

    public DepositPsbt(byte[] depositPsbt) {
        this.depositPsbt = depositPsbt;
    }

    @Override
    public bisq.trade.mu_sig.grpc.DepositPsbt.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.mu_sig.grpc.DepositPsbt.newBuilder()
                .setDepositPsbt(ByteString.copyFrom(depositPsbt));
    }

    @Override
    public bisq.trade.mu_sig.grpc.DepositPsbt toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static DepositPsbt fromProto(bisq.trade.mu_sig.grpc.DepositPsbt proto) {
        return new DepositPsbt(proto.getDepositPsbt().toByteArray());
    }
}
