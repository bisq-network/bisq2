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
import bisq.identity.Identity;
import bisq.offer.Offer;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeState;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public abstract class Trade<T extends Offer<?, ?>, C extends Contract<T>, P extends TradeParty> extends Model implements Proto {

    public static String createId(String offerId, String takerNodeId) {
        return offerId + "." + takerNodeId;
    }

    private final String id;
    private final boolean isBuyer;
    private final boolean isTaker;
    private final Identity myIdentity;
    private final C contract;
    private final P taker;
    private final P maker;

    public Trade(boolean isBuyer, boolean isTaker, Identity myIdentity, C contract, P taker, P maker) {
        this(BisqEasyTradeState.INIT,
                createId(contract.getOffer().getId(), taker.getNetworkId().getId()),
                isBuyer,
                isTaker,
                myIdentity,
                contract,
                taker,
                maker);

    }

    protected Trade(BisqEasyTradeState state, String id, boolean isBuyer, boolean isTaker, Identity myIdentity, C contract, P taker, P maker) {
        super(state);

        this.id = id;
        this.isBuyer = isBuyer;
        this.isTaker = isTaker;
        this.myIdentity = myIdentity;
        this.contract = contract;
        this.taker = taker;
        this.maker = maker;
    }

    protected bisq.trade.protobuf.Trade.Builder getTradeBuilder() {
        return bisq.trade.protobuf.Trade.newBuilder()
                .setId(id)
                .setIsBuyer(isBuyer)
                .setIsTaker(isTaker)
                .setMyIdentity(myIdentity.toProto())
                .setContract(contract.toProto())
                .setTaker(taker.toProto())
                .setMaker(maker.toProto())
                .setState(getState().name());
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

    public T getOffer() {
        return contract.getOffer();
    }

    public P getPeer() {
        return isTaker ? maker : taker;
    }

    public P getMyself() {
        return isTaker ? taker : maker;
    }
}