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
import bisq.contract.Role;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.Trade;
import bisq.trade.TradeParty;
import bisq.trade.TradeRole;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyTrade extends Trade<BisqEasyOffer, BisqEasyContract, BisqEasyTradeParty> {
    @Getter
    private final Observable<String> paymentAccountData = new Observable<>();
    @Getter
    private final Observable<String> btcAddress = new Observable<>();
    @Getter
    private final Observable<String> txId = new Observable<>();

    // The role who cancelled or rejected the trade
    @Getter
    private final Observable<Role> interruptTradeInitiator = new Observable<>();

    private final transient Observable<BisqEasyTradeState> tradeState = new Observable<>();

    public BisqEasyTrade(boolean isBuyer,
                         boolean isTaker,
                         Identity myIdentity,
                         BisqEasyOffer offer,
                         NetworkId takerNetworkId,
                         NetworkId makerNetworkId) {
        super(BisqEasyTradeState.INIT,
                isBuyer,
                isTaker,
                myIdentity,
                offer,
                new BisqEasyTradeParty(takerNetworkId),
                new BisqEasyTradeParty(makerNetworkId));

        stateObservable().addObserver(s -> tradeState.set((BisqEasyTradeState) s));
    }

    private BisqEasyTrade(BisqEasyTradeState state,
                          String id,
                          TradeRole tradeRole,
                          Identity myIdentity,
                          BisqEasyTradeParty taker,
                          BisqEasyTradeParty maker) {
        super(state, id, tradeRole, myIdentity, taker, maker);

        stateObservable().addObserver(s -> tradeState.set((BisqEasyTradeState) s));
    }

    @Override
    public bisq.trade.protobuf.Trade toProto() {
        bisq.trade.protobuf.BisqEasyTrade.Builder builder = bisq.trade.protobuf.BisqEasyTrade.newBuilder();
        Optional.ofNullable(paymentAccountData.get()).ifPresent(builder::setPaymentAccountData);
        Optional.ofNullable(btcAddress.get()).ifPresent(builder::setBtcAddress);
        Optional.ofNullable(txId.get()).ifPresent(builder::setTxId);
        Optional.ofNullable(interruptTradeInitiator.get()).ifPresent(e -> builder.setInterruptTradeInitiator(e.toProtoEnum()));
        return getTradeBuilder().setBisqEasyTrade(builder).build();
    }

    public static BisqEasyTrade fromProto(bisq.trade.protobuf.Trade proto) {
        BisqEasyTrade trade = new BisqEasyTrade(ProtobufUtils.enumFromProto(BisqEasyTradeState.class, proto.getState()),
                proto.getId(),
                TradeRole.fromProto(proto.getTradeRole()),
                Identity.fromProto(proto.getMyIdentity()),
                TradeParty.protoToBisqEasyTradeParty(proto.getTaker()),
                TradeParty.protoToBisqEasyTradeParty(proto.getMaker()));
        if (proto.hasContract()) {
            trade.setContract(BisqEasyContract.fromProto(proto.getContract()));
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

        bisq.trade.protobuf.BisqEasyTrade bisqEasyTradeProto = proto.getBisqEasyTrade();
        if (bisqEasyTradeProto.hasPaymentAccountData()) {
            trade.getPaymentAccountData().set(bisqEasyTradeProto.getPaymentAccountData());
        }
        if (bisqEasyTradeProto.hasBtcAddress()) {
            trade.getBtcAddress().set(bisqEasyTradeProto.getBtcAddress());
        }
        if (bisqEasyTradeProto.hasTxId()) {
            trade.getTxId().set(bisqEasyTradeProto.getTxId());
        }
        if (bisqEasyTradeProto.hasInterruptTradeInitiator()) {
            trade.getInterruptTradeInitiator().set(Role.fromProto(bisqEasyTradeProto.getInterruptTradeInitiator()));
        }
        return trade;
    }

    public BisqEasyTradeState getTradeState() {
        return tradeState.get();
    }

    public ReadOnlyObservable<BisqEasyTradeState> tradeStateObservable() {
        return tradeState;
    }
}
