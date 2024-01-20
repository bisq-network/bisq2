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

package bisq.trade.submarine;

import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.util.ProtobufUtils;
import bisq.contract.submarine.SubmarineContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.submarine.SubmarineOffer;
import bisq.trade.Trade;
import bisq.trade.TradeParty;
import bisq.trade.TradeRole;
import bisq.trade.submarine.protocol.SubmarineTradeState;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class SubmarineTrade extends Trade<SubmarineOffer, SubmarineContract, SubmarineTradeParty> {
    private final Observable<SubmarineTradeState> tradeState = new Observable<>();

    public SubmarineTrade(boolean isBuyer,
                          boolean isTaker,
                          Identity myIdentity,
                          SubmarineContract contract,
                          NetworkId takerNetworkId) {
        super(SubmarineTradeState.INIT,
                isBuyer,
                isTaker,
                myIdentity,
                contract,
                new SubmarineTradeParty(takerNetworkId),
                new SubmarineTradeParty(contract.getMaker().getNetworkId()));

        stateObservable().addObserver(s -> tradeState.set((SubmarineTradeState) s));
    }

    private SubmarineTrade(SubmarineTradeState state,
                           String id,
                           TradeRole tradeRole,
                           Identity myIdentity,
                           SubmarineContract contract,
                           SubmarineTradeParty taker,
                           SubmarineTradeParty maker) {
        super(state, id, tradeRole, myIdentity, contract, taker, maker);

        stateObservable().addObserver(s -> tradeState.set((SubmarineTradeState) s));
    }

    @Override
    public bisq.trade.protobuf.Trade toProto() {
        return getTradeBuilder().setSubmarineTrade(bisq.trade.protobuf.SubmarineTrade.newBuilder())
                .build();
    }

    public static SubmarineTrade fromProto(bisq.trade.protobuf.Trade proto) {
        SubmarineTrade trade = new SubmarineTrade(ProtobufUtils.enumFromProto(SubmarineTradeState.class, proto.getState()),
                proto.getId(),
                TradeRole.fromProto(proto.getTradeRole()),
                Identity.fromProto(proto.getMyIdentity()),
                SubmarineContract.fromProto(proto.getContract()),
                TradeParty.protoToSubmarineTradeParty(proto.getTaker()),
                TradeParty.protoToSubmarineTradeParty(proto.getMaker()));
        if (proto.hasErrorMessage()) {
            trade.setErrorMessage(proto.getErrorMessage());
        }
        if (proto.hasPeersErrorMessage()) {
            trade.setPeersErrorMessage(proto.getPeersErrorMessage());
        }
        return trade;
    }

    public SubmarineTradeState getTradeState() {
        return tradeState.get();
    }

    public ReadOnlyObservable<SubmarineTradeState> tradeStateObservable() {
        return tradeState;
    }
}