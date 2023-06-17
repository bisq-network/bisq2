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

package bisq.trade_protocol;

import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.contract.Contract;
import bisq.network.NetworkId;
import bisq.offer.Offer;
import bisq.trade_protocol.bisq_easy.BisqEasyTradeModel;
import bisq.trade_protocol.fsm.Model;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public abstract class TradeModel<
        O extends Offer<?, ?>,
        C extends Contract<O>>
        extends Model implements Proto {
    public static String createId(String offerId, String takerNodeId) {
        return offerId + "." + takerNodeId;
    }

    private final String id;
    private final C contract;
    private final TradeProtocolParty taker;
    private final TradeProtocolParty maker;

    public TradeModel(C contract, NetworkId takerNetworkId) {
        this(createId(contract.getOffer().getId(), takerNetworkId.getNodeId()),
                contract,
                new TradeProtocolParty(takerNetworkId),
                new TradeProtocolParty(contract.getOffer().getMakerNetworkId()));
    }

    protected TradeModel(String id, C contract, TradeProtocolParty taker, TradeProtocolParty maker) {
        this.id = id;
        this.contract = contract;
        this.taker = taker;
        this.maker = maker;
    }

    protected bisq.protocol.protobuf.TradeModel.Builder getTradeModelBuilder() {
        return bisq.protocol.protobuf.TradeModel.newBuilder()
                .setId(id)
                .setContract(contract.toProto())
                .setTaker(taker.toProto())
                .setMaker(maker.toProto())
                .setState(currentState.get().name());
    }

    public static BisqEasyTradeModel protoToBisqEasyTradeModel(bisq.protocol.protobuf.TradeModel proto) {
        switch (proto.getMessageCase()) {
            case BISQEASYTRADEMODEL: {
                return BisqEasyTradeModel.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public O getOffer() {
        return contract.getOffer();
    }
}