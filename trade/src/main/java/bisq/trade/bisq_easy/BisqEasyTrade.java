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
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufEnumException;
import bisq.contract.Role;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.Trade;
import bisq.trade.TradeLifecycleState;
import bisq.trade.TradeParty;
import bisq.trade.TradeRole;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyTrade extends Trade<BisqEasyOffer, BisqEasyContract, BisqEasyTradeParty> {
    private final Observable<Optional<String>> paymentAccountData = new Observable<>(Optional.empty());
    private final Observable<Optional<String>> bitcoinPaymentData = new Observable<>(Optional.empty()); // btc address in case of mainChain, or LN invoice if LN is used
    private final Observable<Optional<String>> paymentProof = new Observable<>(Optional.empty()); // txId in case of mainChain, or preimage if LN is used

    // The role who cancelled or rejected the trade
    @Getter
    private final Observable<Role> interruptTradeInitiator = new Observable<>();

    // Wrapper for stateObservable which is not handled as generic in Fsm
    private final transient Observable<BisqEasyTradeState> tradeState = new Observable<>();

    @EqualsAndHashCode.Exclude
    private final Observable<Optional<Long>> tradeCompletedDate = new Observable<>(Optional.empty());

    public BisqEasyTrade(BisqEasyContract contract,
                         boolean isBuyer,
                         boolean isTaker,
                         Identity myIdentity,
                         BisqEasyOffer offer,
                         NetworkId takerNetworkId,
                         NetworkId makerNetworkId) {
        super(contract,
                BisqEasyTradeState.INIT,
                isBuyer,
                isTaker,
                myIdentity,
                offer,
                new BisqEasyTradeParty(takerNetworkId),
                new BisqEasyTradeParty(makerNetworkId),
                TradeLifecycleState.ACTIVE);

        stateObservable().addObserver(state -> tradeState.set((BisqEasyTradeState) state));
    }

    private BisqEasyTrade(BisqEasyContract contract,
                          BisqEasyTradeState state,
                          String id,
                          TradeRole tradeRole,
                          Identity myIdentity,
                          BisqEasyTradeParty taker,
                          BisqEasyTradeParty maker,
                          TradeLifecycleState lifecycleState) {
        super(contract, state, id, tradeRole, myIdentity, taker, maker, lifecycleState);

        stateObservable().addObserver(s -> tradeState.set((BisqEasyTradeState) s));
    }

    @Override
    public bisq.trade.protobuf.Trade.Builder getBuilder(boolean serializeForHash) {
        return getTradeBuilder(serializeForHash).setBisqEasyTrade(toBisqEasyTradeProto(serializeForHash));
    }

    @Override
    public bisq.trade.protobuf.Trade toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    private bisq.trade.protobuf.BisqEasyTrade toBisqEasyTradeProto(boolean serializeForHash) {
        return resolveBuilder(getBisqEasyTradeBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.trade.protobuf.BisqEasyTrade.Builder getBisqEasyTradeBuilder(boolean serializeForHash) {
        var builder = bisq.trade.protobuf.BisqEasyTrade.newBuilder();
        getPaymentAccountData().ifPresent(builder::setPaymentAccountData);
        getBitcoinPaymentData().ifPresent(builder::setBitcoinPaymentData);
        getPaymentProof().ifPresent(builder::setPaymentProof);
        Optional.ofNullable(interruptTradeInitiator.get()).ifPresent(e -> builder.setInterruptTradeInitiator(e.toProtoEnum()));
        getTradeCompletedDate().ifPresent(builder::setTradeCompletedDate);
        return builder;
    }

    public static BisqEasyTrade fromProto(bisq.trade.protobuf.Trade proto) throws UnresolvableProtobufEnumException {
        BisqEasyTradeState state = ProtobufUtils.enumFromProto(BisqEasyTradeState.class, proto.getState());
        BisqEasyTrade trade = new BisqEasyTrade(BisqEasyContract.fromProto(proto.getContract()),
                state,
                proto.getId(),
                TradeRole.fromProto(proto.getTradeRole()),
                Identity.fromProto(proto.getMyIdentity()),
                TradeParty.protoToBisqEasyTradeParty(proto.getTaker()),
                TradeParty.protoToBisqEasyTradeParty(proto.getMaker()),
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
        if (proto.hasTradeProtocolFailure()) {
            trade.setTradeProtocolFailure(TradeProtocolFailure.fromProto(proto.getTradeProtocolFailure()));
        }
        if (proto.hasPeersTradeProtocolFailure()) {
            trade.setPeersTradeProtocolFailure(TradeProtocolFailure.fromProto(proto.getPeersTradeProtocolFailure()));
        }

        bisq.trade.protobuf.BisqEasyTrade bisqEasyTradeProto = proto.getBisqEasyTrade();
        if (bisqEasyTradeProto.hasPaymentAccountData()) {
            trade.setPaymentAccountData(Optional.of(bisqEasyTradeProto.getPaymentAccountData()));
        }
        if (bisqEasyTradeProto.hasBitcoinPaymentData()) {
            trade.setBitcoinPaymentData(Optional.of(bisqEasyTradeProto.getBitcoinPaymentData()));
        }
        if (bisqEasyTradeProto.hasPaymentProof()) {
            trade.setPaymentProof(Optional.of(bisqEasyTradeProto.getPaymentProof()));
        }
        if (bisqEasyTradeProto.hasInterruptTradeInitiator()) {
            trade.getInterruptTradeInitiator().set(Role.fromProto(bisqEasyTradeProto.getInterruptTradeInitiator()));
        }
        if (bisqEasyTradeProto.hasTradeCompletedDate()) {
            trade.setTradeCompletedDate(Optional.of(bisqEasyTradeProto.getTradeCompletedDate()));
        }

        return trade;
    }

    public BisqEasyTradeState getTradeState() {
        return tradeState.get();
    }

    public ReadOnlyObservable<BisqEasyTradeState> tradeStateObservable() {
        return tradeState;
    }

    public Optional<String> getPaymentAccountData() {
        return paymentAccountData.get();
    }

    public void setPaymentAccountData(Optional<String> value) {
        paymentAccountData.set(checkNotNull(value));
    }

    public ReadOnlyObservable<Optional<String>> paymentAccountDataObservable() {
        return paymentAccountData;
    }

    public Optional<String> getBitcoinPaymentData() {
        return bitcoinPaymentData.get();
    }

    public void setBitcoinPaymentData(Optional<String> value) {
        bitcoinPaymentData.set(checkNotNull(value));
    }

    public ReadOnlyObservable<Optional<String>> bitcoinPaymentDataObservable() {
        return bitcoinPaymentData;
    }

    public Optional<String> getPaymentProof() {
        return paymentProof.get();
    }

    public void setPaymentProof(Optional<String> value) {
        paymentProof.set(checkNotNull(value));
    }

    public ReadOnlyObservable<Optional<String>> paymentProofObservable() {
        return paymentProof;
    }

    public Optional<Long> getTradeCompletedDate() {
        return tradeCompletedDate.get();
    }

    public void setTradeCompletedDate(Optional<Long> value) {
        tradeCompletedDate.set(value);
    }

    public ReadOnlyObservable<Optional<Long>> tradeCompletedDateObservable() {
        return tradeCompletedDate;
    }
}
