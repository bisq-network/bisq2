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

package bisq.trade.multisig;

import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.util.ProtobufUtils;
import bisq.contract.multisig.MultisigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.multisig.MultisigOffer;
import bisq.trade.Trade;
import bisq.trade.TradeParty;
import bisq.trade.TradeRole;
import bisq.trade.multisig.protocol.MultisigTradeState;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class MultisigTrade extends Trade<MultisigOffer, MultisigContract, MultisigTradeParty> {
    private final Observable<MultisigTradeState> tradeState = new Observable<>();

    public MultisigTrade(boolean isBuyer,
                         boolean isTaker,
                         Identity myIdentity,
                         MultisigContract contract,
                         NetworkId takerNetworkId) {
        super(MultisigTradeState.INIT,
                isBuyer,
                isTaker,
                myIdentity,
                contract.getOffer(),
                new MultisigTradeParty(takerNetworkId),
                new MultisigTradeParty(contract.getMaker().getNetworkId()));

        setContract(contract);
        stateObservable().addObserver(s -> tradeState.set((MultisigTradeState) s));
    }

    private MultisigTrade(MultisigTradeState state,
                          String id,
                          TradeRole tradeRole,
                          Identity myIdentity,
                          MultisigTradeParty taker,
                          MultisigTradeParty maker) {
        super(state, id, tradeRole, myIdentity, taker, maker);

        stateObservable().addObserver(s -> tradeState.set((MultisigTradeState) s));
    }

    @Override
    public bisq.trade.protobuf.Trade.Builder getBuilder(boolean ignoreAnnotation) {
        return getTradeBuilder(ignoreAnnotation).setMultisigTrade(bisq.trade.protobuf.MultisigTrade.newBuilder());
    }

    @Override
    public bisq.trade.protobuf.Trade toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static MultisigTrade fromProto(bisq.trade.protobuf.Trade proto) {
        MultisigTrade trade = new MultisigTrade(ProtobufUtils.enumFromProto(MultisigTradeState.class, proto.getState()),
                proto.getId(),
                TradeRole.fromProto(proto.getTradeRole()),
                Identity.fromProto(proto.getMyIdentity()),
                TradeParty.protoToMultisigTradeParty(proto.getTaker()),
                TradeParty.protoToMultisigTradeParty(proto.getMaker()));
        if (proto.hasContract()) {
            trade.setContract(MultisigContract.fromProto(proto.getContract()));
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

    public MultisigTradeState getTradeState() {
        return tradeState.get();
    }

    public ReadOnlyObservable<MultisigTradeState> tradeStateObservable() {
        return tradeState;
    }
}