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

package bisq.trade;

import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.contract.ContractSignatureData;
import bisq.network.NetworkId;
import bisq.trade.bisq_easy.BisqEasyTradeParty;
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
public class TradeParty implements Proto {
    private final NetworkId networkId;
    @Setter
    @Nullable
    private ContractSignatureData contractSignatureData;
    @Setter
    @Nullable
    private String paymentAccountData;

    public TradeParty(NetworkId networkId) {
        this.networkId = networkId;
    }

    @Override
    public bisq.trade.protobuf.TradeParty toProto() {
        bisq.trade.protobuf.TradeParty.Builder builder = bisq.trade.protobuf.TradeParty.newBuilder()
                .setNetworkId(networkId.toProto());
        Optional.ofNullable(contractSignatureData).ifPresent(ContractSignatureData::toProto);
        return builder.build();
    }

    public static BisqEasyTradeParty protoToBisqEasyTradeParty(bisq.trade.protobuf.TradeParty proto) {
        switch (proto.getMessageCase()) {
            case BISQEASYTRADEPARTY: {
                return BisqEasyTradeParty.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}