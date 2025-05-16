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

package bisq.trade.mu_sig;

import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.proto.ProtobufUtils;
import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.trade.Trade;
import bisq.trade.TradeLifecycleState;
import bisq.trade.TradeParty;
import bisq.trade.TradeRole;
import bisq.trade.mu_sig.messages.grpc.TxConfirmationStatus;
import bisq.trade.mu_sig.protocol.MuSigTradeState;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.trade.mu_sig.protocol.MuSigTradeState.DEPOSIT_TX_CONFIRMED;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.INIT;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class MuSigTrade extends Trade<MuSigOffer, MuSigContract, MuSigTradeParty> {

    //todo use ReadOnlyObservable
    // The role who cancelled or rejected the trade
    @Getter
    private final Observable<Role> interruptTradeInitiator = new Observable<>();

    // Wrapper for stateObservable which is not handled as generic in Fsm
    private final transient Observable<MuSigTradeState> tradeState = new Observable<>();

    private final Observable<TxConfirmationStatus> depositTxConfirmationStatus = new Observable<>();

    //todo temp
    @Getter
    private final Observable<String> paymentAccountData = new Observable<>("TODO paymentAccountData");
    @Getter
    private final Observable<String> btcAddress = new Observable<>("TODO btcAddress");
    // paymentProof can be null in Observable

    @Getter
    private final Observable<String> depositTxId = new Observable<>("TODO depositTxId");

    @Setter
    @Getter
    private Optional<Long> tradeCompletedDate = Optional.empty();

    public MuSigTrade(MuSigContract contract,
                      boolean isBuyer,
                      boolean isTaker,
                      Identity myIdentity,
                      MuSigOffer offer,
                      NetworkId takerNetworkId,
                      NetworkId makerNetworkId) {
        super(contract,
                MuSigTradeState.INIT,
                isBuyer,
                isTaker,
                myIdentity,
                offer,
                new MuSigTradeParty(takerNetworkId),
                new MuSigTradeParty(makerNetworkId),
                TradeLifecycleState.ACTIVE);

        stateObservable().addObserver(state -> tradeState.set((MuSigTradeState) state));
    }

    public MuSigTrade(MuSigContract contract,
                      MuSigTradeState state,
                      String id,
                      TradeRole tradeRole,
                      Identity myIdentity,
                      MuSigTradeParty taker,
                      MuSigTradeParty maker,
                      TradeLifecycleState lifecycleState) {
        super(contract, state, id, tradeRole, myIdentity, taker, maker,lifecycleState);

        stateObservable().addObserver(s -> tradeState.set((MuSigTradeState) s));
    }


    @Override
    public bisq.trade.protobuf.Trade.Builder getBuilder(boolean serializeForHash) {
        return getTradeBuilder(serializeForHash).setMuSigTrade(toMuSigTradeProto(serializeForHash));
    }

    @Override
    public bisq.trade.protobuf.Trade toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    private bisq.trade.protobuf.MuSigTrade toMuSigTradeProto(boolean serializeForHash) {
        return resolveBuilder(getMuSigTradeBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.trade.protobuf.MuSigTrade.Builder getMuSigTradeBuilder(boolean serializeForHash) {
        var builder = bisq.trade.protobuf.MuSigTrade.newBuilder();
        Optional.ofNullable(interruptTradeInitiator.get()).ifPresent(e -> builder.setInterruptTradeInitiator(e.toProtoEnum()));
        tradeCompletedDate.ifPresent(builder::setTradeCompletedDate);
        return builder;
    }

    public static MuSigTrade fromProto(bisq.trade.protobuf.Trade proto) {
        bisq.trade.protobuf.MuSigTrade muSigTradeProto = proto.getMuSigTrade();
        MuSigTrade trade = new MuSigTrade(MuSigContract.fromProto(proto.getContract()),
                ProtobufUtils.enumFromProto(MuSigTradeState.class, proto.getState()),
                proto.getId(),
                TradeRole.fromProto(proto.getTradeRole()),
                Identity.fromProto(proto.getMyIdentity()),
                TradeParty.protoToMuSigTradeParty(proto.getTaker()),
                TradeParty.protoToMuSigTradeParty(proto.getMaker()),
                TradeLifecycleState.fromProto(proto.getLifecycleState()));
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

        if (muSigTradeProto.hasInterruptTradeInitiator()) {
            trade.getInterruptTradeInitiator().set(Role.fromProto(muSigTradeProto.getInterruptTradeInitiator()));
        }
        if (muSigTradeProto.hasTradeCompletedDate()) {
            trade.setTradeCompletedDate(Optional.of(muSigTradeProto.getTradeCompletedDate()));
        }
        return trade;
    }

    public MuSigTradeState getTradeState() {
        return tradeState.get();
    }

    public ReadOnlyObservable<MuSigTradeState> tradeStateObservable() {
        return tradeState;
    }

    public boolean isDepositTxCreatedButNotConfirmed() {
        // TODO make more rigid check
        log.error(tradeState.get().name());
        return /*getMyself().getDepositPsbt() != null &&*/  // todo not persisted yet
                tradeState.get().ordinal() > INIT.ordinal() &&
                        tradeState.get().ordinal() < DEPOSIT_TX_CONFIRMED.ordinal();
    }

    public void setDepositTxConfirmationStatus(TxConfirmationStatus txConfirmationStatus) {
        depositTxConfirmationStatus.set(txConfirmationStatus);

    }

    public ReadOnlyObservable<TxConfirmationStatus> getDepositTxConfirmationStatus() {
        return depositTxConfirmationStatus;
    }

}