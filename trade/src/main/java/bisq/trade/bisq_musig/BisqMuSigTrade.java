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

package bisq.trade.bisq_musig;

import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.proto.ProtobufUtils;
import bisq.contract.bisq_musig.BisqMuSigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_musig.BisqMuSigOffer;
import bisq.trade.Trade;
import bisq.trade.TradeParty;
import bisq.trade.TradeRole;
import bisq.trade.bisq_musig.protocol.BisqMuSigTradeState;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class BisqMuSigTrade extends Trade<BisqMuSigOffer, BisqMuSigContract, BisqMuSigTradeParty> {
    private final Observable<BisqMuSigTradeState> tradeState = new Observable<>();

    public BisqMuSigTrade(boolean isBuyer,
                          boolean isTaker,
                          Identity myIdentity,
                          BisqMuSigContract contract,
                          NetworkId takerNetworkId) {
        super(BisqMuSigTradeState.INIT,
                isBuyer,
                isTaker,
                myIdentity,
                contract.getOffer(),
                new BisqMuSigTradeParty(takerNetworkId),
                new BisqMuSigTradeParty(contract.getMaker().getNetworkId()));

        setContract(contract);
        stateObservable().addObserver(s -> tradeState.set((BisqMuSigTradeState) s));
    }

    private BisqMuSigTrade(BisqMuSigTradeState state,
                           String id,
                           TradeRole tradeRole,
                           Identity myIdentity,
                           BisqMuSigTradeParty taker,
                           BisqMuSigTradeParty maker) {
        super(state, id, tradeRole, myIdentity, taker, maker);

        stateObservable().addObserver(s -> tradeState.set((BisqMuSigTradeState) s));
    }

    @Override
    public bisq.trade.protobuf.Trade.Builder getBuilder(boolean serializeForHash) {
        return getTradeBuilder(serializeForHash).setBisqMuSigTrade(bisq.trade.protobuf.BisqMuSigTrade.newBuilder());
    }

    @Override
    public bisq.trade.protobuf.Trade toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BisqMuSigTrade fromProto(bisq.trade.protobuf.Trade proto) {
        BisqMuSigTrade trade = new BisqMuSigTrade(ProtobufUtils.enumFromProto(BisqMuSigTradeState.class, proto.getState()),
                proto.getId(),
                TradeRole.fromProto(proto.getTradeRole()),
                Identity.fromProto(proto.getMyIdentity()),
                TradeParty.protoToBisqMuSigTradeParty(proto.getTaker()),
                TradeParty.protoToBisqMuSigTradeParty(proto.getMaker()));
        if (proto.hasContract()) {
            trade.setContract(BisqMuSigContract.fromProto(proto.getContract()));
        }
        if (proto.hasErrorMessage()) {
            trade.setErrorMessage(proto.getErrorMessage());
        }
        if (proto.hasErrorStackTrace()) {
            trade.setErrorStackTrace(proto.getErrorStackTrace());
        }
        if (proto.hasPeersErrorMessage()) {
            trade.setPeersErrorMessage(proto.getPeersErrorMessage());
        }
        if (proto.hasPeersErrorStackTrace()) {
            trade.setPeersErrorStackTrace(proto.getPeersErrorStackTrace());
        }
        return trade;
    }

    public BisqMuSigTradeState getTradeState() {
        return tradeState.get();
    }

    public ReadOnlyObservable<BisqMuSigTradeState> tradeStateObservable() {
        return tradeState;
    }
}