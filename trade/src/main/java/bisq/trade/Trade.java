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
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.proto.PersistableProto;
import bisq.contract.Contract;
import bisq.identity.Identity;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.security.DigestUtil;
import bisq.trade.exceptions.TradeProtocolFailure;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class Trade<T extends Offer<?, ?>, C extends Contract<T>, P extends TradeParty> extends FsmModel implements PersistableProto {
    public static String createId(String offerId, String takerPubKeyHash) {
        return createId(offerId, takerPubKeyHash, Optional.empty());
    }

    public static String createId(String offerId, String takerPubKeyHash, long takeOfferDate) {
        return createId(offerId, takerPubKeyHash, Optional.of(takeOfferDate));
    }

    public static String createId(String offerId, String takerPubKeyHash, Optional<Long> takeOfferDate) {
        String combined = offerId + takerPubKeyHash + takeOfferDate.map(String::valueOf).orElse("");
        return UUID.nameUUIDFromBytes(DigestUtil.hash(combined.getBytes(StandardCharsets.UTF_8))).toString();
    }

    private static TradeRole createRole(boolean isBuyer, boolean isTaker) {
        return isBuyer ?
                (isTaker ?
                        TradeRole.BUYER_AS_TAKER :
                        TradeRole.BUYER_AS_MAKER) :
                (isTaker ?
                        TradeRole.SELLER_AS_TAKER :
                        TradeRole.SELLER_AS_MAKER);
    }

    private final String id;
    private final Identity myIdentity;
    private final P taker;
    private final P maker;
    // transient fields are excluded by default for EqualsAndHashCode
    private transient final TradeRole tradeRole;
    private final C contract;
    private final Observable<String> errorMessage = new Observable<>();
    private final Observable<String> errorStackTrace = new Observable<>();
    private final Observable<TradeProtocolFailure> tradeProtocolFailure = new Observable<>();
    private final Observable<String> peersErrorMessage = new Observable<>();
    private final Observable<String> peersErrorStackTrace = new Observable<>();
    private final Observable<TradeProtocolFailure> peersTradeProtocolFailure = new Observable<>();

    // Set at protocol creation and not updated later, thus no need to be observable
    private final Observable<String> protocolVersion = new Observable<>();

    private final Observable<TradeLifecycleState> lifecycleState = new Observable<>();

    public Trade(C contract,
                 State state,
                 boolean isBuyer,
                 boolean isTaker,
                 Identity myIdentity,
                 T offer,
                 P taker,
                 P maker,
                 TradeLifecycleState lifecycleState) {
        this(contract,
                state,
                createId(offer.getId(), taker.getNetworkId().getId(), contract.getTakeOfferDate()),
                createRole(isBuyer, isTaker),
                myIdentity,
                taker,
                maker,
                lifecycleState);
    }

    protected Trade(C contract,
                    State state,
                    String id,
                    TradeRole tradeRole,
                    Identity myIdentity,
                    P taker,
                    P maker,
                    TradeLifecycleState lifecycleState) {
        super(state);

        this.contract = contract;
        this.id = id;
        this.tradeRole = tradeRole;
        this.myIdentity = myIdentity;
        this.taker = taker;
        this.maker = maker;
        this.setLifecycleState(lifecycleState);
    }

    protected bisq.trade.protobuf.Trade.Builder getTradeBuilder(boolean serializeForHash) {
        bisq.trade.protobuf.Trade.Builder builder = bisq.trade.protobuf.Trade.newBuilder()
                .setContract(contract.toProto(serializeForHash))
                .setId(id)
                .setTradeRole(tradeRole.toProtoEnum())
                .setMyIdentity(myIdentity.toProto(serializeForHash))
                .setTaker(taker.toProto(serializeForHash))
                .setMaker(maker.toProto(serializeForHash))
                .setState(getState().name())
                .setLifecycleState(getLifecycleState().toProtoEnum());
        Optional.ofNullable(getErrorMessage()).ifPresent(builder::setErrorMessage);
        Optional.ofNullable(getErrorStackTrace()).ifPresent(builder::setErrorStackTrace);
        Optional.ofNullable(getPeersErrorMessage()).ifPresent(builder::setPeersErrorMessage);
        Optional.ofNullable(getPeersErrorStackTrace()).ifPresent(builder::setPeersErrorStackTrace);
        Optional.ofNullable(getTradeProtocolFailure()).ifPresent(e -> builder.setTradeProtocolFailure(e.toProtoEnum()));
        Optional.ofNullable(getPeersTradeProtocolFailure()).ifPresent(e -> builder.setPeersTradeProtocolFailure(e.toProtoEnum()));
        return builder;
    }

    protected void setErrorMessage(String errorMessage) {
        this.errorMessage.set(errorMessage);
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage.get();
    }

    public ReadOnlyObservable<String> errorMessageObservable() {
        return errorMessage;
    }

    protected void setErrorStackTrace(String peersErrorStacktrace) {
        this.errorStackTrace.set(peersErrorStacktrace);
    }

    @Nullable
    public String getErrorStackTrace() {
        return errorStackTrace.get();
    }

    public ReadOnlyObservable<String> errorStackTraceObservable() {
        return errorStackTrace;
    }

    protected void setTradeProtocolFailure(TradeProtocolFailure tradeProtocolFailure) {
        this.tradeProtocolFailure.set(tradeProtocolFailure);
    }

    @Nullable
    public TradeProtocolFailure getTradeProtocolFailure() {
        return tradeProtocolFailure.get();
    }

    public ReadOnlyObservable<TradeProtocolFailure> tradeProtocolFailureObservable() {
        return tradeProtocolFailure;
    }


    public void setErrorData(TradeProtocolFailure tradeProtocolFailure,
                             String errorStackTrace,
                             String errorMessage) {
        this.tradeProtocolFailure.set(tradeProtocolFailure);
        this.errorStackTrace.set(errorStackTrace);
        this.errorMessage.set(errorMessage);
    }


    protected void setPeersErrorMessage(String peersErrorMessage) {
        this.peersErrorMessage.set(peersErrorMessage);
    }

    @Nullable
    public String getPeersErrorMessage() {
        return peersErrorMessage.get();
    }

    public ReadOnlyObservable<String> peersErrorMessageObservable() {
        return peersErrorMessage;
    }

    protected void setPeersErrorStackTrace(String peersErrorStackTrace) {
        this.peersErrorStackTrace.set(peersErrorStackTrace);
    }

    @Nullable
    public String getPeersErrorStackTrace() {
        return peersErrorStackTrace.get();
    }

    public ReadOnlyObservable<String> peersErrorStackTraceObservable() {
        return peersErrorStackTrace;
    }

    protected void setPeersTradeProtocolFailure(TradeProtocolFailure peersTradeProtocolFailure) {
        this.peersTradeProtocolFailure.set(peersTradeProtocolFailure);
    }

    @Nullable
    public TradeProtocolFailure getPeersTradeProtocolFailure() {
        return peersTradeProtocolFailure.get();
    }

    public ReadOnlyObservable<TradeProtocolFailure> peersTradeProtocolFailureObservable() {
        return peersTradeProtocolFailure;
    }


    public void setPeersErrorData(TradeProtocolFailure peersTradeProtocolFailure,
                                  String peersErrorStackTrace,
                                  String peersErrorMessage) {
        this.peersTradeProtocolFailure.set(peersTradeProtocolFailure);
        this.peersErrorStackTrace.set(peersErrorStackTrace);
        this.peersErrorMessage.set(peersErrorMessage);
    }


    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion.set(protocolVersion);
    }

    public String getProtocolVersion() {
        return this.protocolVersion.get();
    }

    public void setLifecycleState(TradeLifecycleState lifecycleState) {
        this.lifecycleState.set(lifecycleState);
    }

    public TradeLifecycleState getLifecycleState() {
        return this.lifecycleState.get();
    }

    public ReadOnlyObservable<TradeLifecycleState> lifecycleState() {
        return lifecycleState;
    }

    /* --------------------------------------------------------------------- */
    // Delegates
    /* --------------------------------------------------------------------- */

    public T getOffer() {
        return contract.getOffer();
    }

    public boolean isBuyer() {
        return tradeRole.isBuyer();
    }

    public boolean isSeller() {
        return tradeRole.isSeller();
    }

    public boolean isTaker() {
        return tradeRole.isTaker();
    }

    public boolean isMaker() {
        return tradeRole.isMaker();
    }

    public P getPeer() {
        return tradeRole.isTaker() ? maker : taker;
    }

    public P getMyself() {
        return tradeRole.isTaker() ? taker : maker;
    }

    public P getBuyer() {
        if (tradeRole == TradeRole.BUYER_AS_TAKER) {
            return taker;
        } else if (tradeRole == TradeRole.BUYER_AS_MAKER) {
            return maker;
        } else if (tradeRole == TradeRole.SELLER_AS_TAKER) {
            return maker;
        } else {
            // tradeRole == TradeRole.SELLER_AS_MAKER
            return taker;
        }
    }

    public P getSeller() {
        if (tradeRole == TradeRole.BUYER_AS_TAKER) {
            return maker;
        } else if (tradeRole == TradeRole.BUYER_AS_MAKER) {
            return taker;
        } else if (tradeRole == TradeRole.SELLER_AS_TAKER) {
            return taker;
        } else {
            // tradeRole == TradeRole.SELLER_AS_MAKER
            return maker;
        }
    }

    public String getShortId() {
        return id.substring(0, 8);
    }

    public Direction getDisplayDirection() {
        boolean isBaseCurrencyBitcoin = getOffer().getMarket().isBaseCurrencyBitcoin();
        Direction domainDirection = getDirection();
        return isBaseCurrencyBitcoin ? domainDirection : domainDirection.mirror();
    }

    public Direction getDirection() {
        return switch (tradeRole) {
            case BUYER_AS_TAKER, BUYER_AS_MAKER -> Direction.BUY;
            case SELLER_AS_TAKER, SELLER_AS_MAKER -> Direction.SELL;
        };
    }
}
