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
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ReceiverAddressAndAmount implements Proto {
    private final String address;
    private final long amount;

    public ReceiverAddressAndAmount(String address, long amount) {
        this.address = address;
        this.amount = amount;
    }

    @Override
    public bisq.trade.protobuf.ReceiverAddressAndAmount.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.ReceiverAddressAndAmount.newBuilder()
                .setAddress(address)
                .setAmount(amount);
    }

    @Override
    public bisq.trade.protobuf.ReceiverAddressAndAmount toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static ReceiverAddressAndAmount fromProto(bisq.trade.protobuf.ReceiverAddressAndAmount proto) {
        return new ReceiverAddressAndAmount(proto.getAddress(), proto.getAmount());
    }
}
