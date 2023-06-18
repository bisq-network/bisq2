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

import bisq.common.fsm.Model;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.contract.Contract;
import bisq.network.NetworkId;
import bisq.offer.Offer;
import bisq.trade.bisq_easy.BisqEasyTrade;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public abstract class Trade<
        O extends Offer<?, ?>,
        C extends Contract<O>>
        extends Model implements Proto {
    public static String createId(String offerId, String takerNodeId) {
        return offerId + "." + takerNodeId;
    }

    private final String id;
    private final C contract;
    private final TradeParty taker;
    private final TradeParty maker;

    public Trade(C contract, NetworkId takerNetworkId) {
        this(createId(contract.getOffer().getId(), takerNetworkId.getId()),
                contract,
                new TradeParty(takerNetworkId),
                new TradeParty(contract.getOffer().getMakerNetworkId()));
    }

    protected Trade(String id, C contract, TradeParty taker, TradeParty maker) {
        this.id = id;
        this.contract = contract;
        this.taker = taker;
        this.maker = maker;
    }

    protected bisq.trade.protobuf.Trade.Builder getTradeBuilder() {
        return bisq.trade.protobuf.Trade.newBuilder()
                .setId(id)
                .setContract(contract.toProto())
                .setTaker(taker.toProto())
                .setMaker(maker.toProto())
                .setState(currentState.get().name());
    }

    public static BisqEasyTrade protoToBisqEasyTrade(bisq.trade.protobuf.Trade proto) {
        switch (proto.getMessageCase()) {
            case BISQEASYTRADE: {
                return BisqEasyTrade.fromProto(proto);
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