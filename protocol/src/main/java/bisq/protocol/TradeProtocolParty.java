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

package bisq.protocol;

import bisq.common.proto.Proto;
import bisq.contract.ContractSignatureData;
import bisq.network.NetworkId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public class TradeProtocolParty implements Proto {
    private final NetworkId networkId;
    @Setter
    @Nullable
    private ContractSignatureData contractSignatureData;

    public TradeProtocolParty(NetworkId networkId) {
        this.networkId = networkId;
    }

    @Override
    public bisq.protocol.protobuf.TradeProtocolParty toProto() {
        bisq.protocol.protobuf.TradeProtocolParty.Builder builder = bisq.protocol.protobuf.TradeProtocolParty.newBuilder()
                .setNetworkId(networkId.toProto());
        Optional.ofNullable(contractSignatureData).ifPresent(ContractSignatureData::toProto);
        return builder.build();
    }

    public static TradeProtocolParty fromProto(bisq.protocol.protobuf.TradeProtocolParty proto) {
        TradeProtocolParty tradeProtocolParty = new TradeProtocolParty(NetworkId.fromProto(proto.getNetworkId()));
        if (proto.hasContractSignatureData()) {
            tradeProtocolParty.setContractSignatureData(ContractSignatureData.fromProto(proto.getContractSignatureData()));
        }
        return tradeProtocolParty;
    }
}