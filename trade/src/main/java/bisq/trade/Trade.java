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

import bisq.common.fsm.FsmModel;
import bisq.common.fsm.State;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.contract.Contract;
import bisq.identity.Identity;
import bisq.offer.Offer;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.multisig.MultisigTrade;
import bisq.trade.submarine.SubmarineTrade;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class Trade<T extends Offer<?, ?>, C extends Contract<T>, P extends TradeParty> extends FsmModel implements Proto {
    @Getter
    public enum Role {
        BUYER_AS_TAKER(true, true),
        BUYER_AS_MAKER(true, false),
        SELLER_AS_TAKER(false, true),
        SELLER_AS_MAKER(false, false);

        private final boolean isBuyer;
        private final boolean isTaker;

        Role(boolean isBuyer, boolean isTaker) {
            this.isBuyer = isBuyer;
            this.isTaker = isTaker;
        }

        public boolean isMaker() {
            return !isTaker;
        }

        public boolean isSeller() {
            return !isBuyer;
        }
    }

    public static String createId(String offerId, String takerNodeId) {
        return offerId + "." + takerNodeId;
    }

    @Getter
    private final String id;
    private final boolean isBuyer;
    private final boolean isTaker;
    @Getter
    private final Identity myIdentity;
    @Getter
    private final C contract;
    @Getter
    private final P taker;
    @Getter
    private final P maker;
    @Getter
    private transient final Role role;

    public Trade(State state, boolean isBuyer, boolean isTaker, Identity myIdentity, C contract, P taker, P maker) {
        this(state,
                createId(contract.getOffer().getId(), taker.getNetworkId().getId()),
                isBuyer,
                isTaker,
                myIdentity,
                contract,
                taker,
                maker);

    }

    protected Trade(State state, String id, boolean isBuyer, boolean isTaker, Identity myIdentity, C contract, P taker, P maker) {
        super(state);

        this.id = id;
        this.isBuyer = isBuyer;
        this.isTaker = isTaker;
        this.myIdentity = myIdentity;
        this.contract = contract;
        this.taker = taker;
        this.maker = maker;

        role = isBuyer ?
                (isTaker ?
                        Role.BUYER_AS_TAKER :
                        Role.BUYER_AS_MAKER) :
                (isTaker ?
                        Role.SELLER_AS_TAKER :
                        Role.SELLER_AS_MAKER);
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

    public static MultisigTrade protoToMultisigTrade(bisq.trade.protobuf.Trade proto) {
        switch (proto.getMessageCase()) {
            case MULTISIGTRADE: {
                return MultisigTrade.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public static SubmarineTrade protoToSubmarineTrade(bisq.trade.protobuf.Trade proto) {
        switch (proto.getMessageCase()) {
            case SUBMARINETRADE: {
                return SubmarineTrade.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Convenience Getters
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public T getOffer() {
        return contract.getOffer();
    }

    public boolean isBuyer() {
        return isBuyer;
    }

    public boolean isSeller() {
        return !isBuyer;
    }

    public boolean isTaker() {
        return isTaker;
    }

    public boolean isMaker() {
        return !isTaker;
    }

    public P getPeer() {
        return isTaker ? maker : taker;
    }

    public P getMyself() {
        return isTaker ? taker : maker;
    }

    public P getBuyer() {
        return role.isTaker() ? taker : maker;
    }

    public P getSeller() {
        return role.isTaker() ? taker : maker;
    }
}