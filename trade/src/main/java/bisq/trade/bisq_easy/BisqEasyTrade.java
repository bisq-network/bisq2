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

package bisq.trade.bisq_easy;

import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.util.ProtobufUtils;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.Trade;
import bisq.trade.TradeParty;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyTrade extends Trade<BisqEasyOffer, BisqEasyContract, BisqEasyTradeParty> {
    private final Observable<BisqEasyTradeState> tradeState = new Observable<>();

    public BisqEasyTrade(boolean isBuyer,
                         boolean isTaker,
                         Identity myIdentity,
                         BisqEasyContract contract,
                         NetworkId takerNetworkId) {
        super(BisqEasyTradeState.INIT,
                isBuyer,
                isTaker,
                myIdentity,
                contract,
                new BisqEasyTradeParty(takerNetworkId),
                new BisqEasyTradeParty(contract.getMaker().getNetworkId()));

        stateObservable().addObserver(s -> tradeState.set((BisqEasyTradeState) s));
    }

    private BisqEasyTrade(BisqEasyTradeState state,
                          String id,
                          boolean isBuyer,
                          boolean isTaker,
                          Identity myIdentity,
                          BisqEasyContract contract,
                          BisqEasyTradeParty taker,
                          BisqEasyTradeParty maker) {
        super(state, id, isBuyer, isTaker, myIdentity, contract, taker, maker);

        stateObservable().addObserver(s -> tradeState.set((BisqEasyTradeState) s));
    }

    @Override
    public bisq.trade.protobuf.Trade toProto() {
        return getTradeBuilder().setBisqEasyTrade(bisq.trade.protobuf.BisqEasyTrade.newBuilder())
                .build();
    }

    public static BisqEasyTrade fromProto(bisq.trade.protobuf.Trade proto) {
        return new BisqEasyTrade(ProtobufUtils.enumFromProto(BisqEasyTradeState.class, proto.getState()),
                proto.getId(),
                proto.getIsBuyer(),
                proto.getIsTaker(),
                Identity.fromProto(proto.getMyIdentity()),
                BisqEasyContract.fromProto(proto.getContract()),
                TradeParty.protoToBisqEasyTradeParty(proto.getTaker()),
                TradeParty.protoToBisqEasyTradeParty(proto.getMaker()));
    }

    public BisqEasyTradeState getTradeState() {
        return tradeState.get();
    }

    public ReadOnlyObservable<BisqEasyTradeState> tradeStateObservable() {
        return tradeState;
    }
}