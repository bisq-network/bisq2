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
import bisq.offer.Offer;
import bisq.security.DigestUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class Trade<T extends Offer<?, ?>, C extends Contract<T>, P extends TradeParty> extends FsmModel implements PersistableProto {
    public static String createId(String offerId, String takerPubKeyHash) {
        String combined = offerId + takerPubKeyHash;
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
    private final Observable<C> contract = new Observable<>();
    private final Observable<String> errorMessage = new Observable<>();
    private final Observable<String> errorStackTrace = new Observable<>();
    private final Observable<String> peersErrorMessage = new Observable<>();
    private final Observable<String> peersErrorStackTrace = new Observable<>();
    private final Observable<String> protocolVersion = new Observable<>();

    public Trade(State state,
                 boolean isBuyer,
                 boolean isTaker,
                 Identity myIdentity,
                 T offer,
                 P taker,
                 P maker) {
        this(state,
                createId(offer.getId(), taker.getNetworkId().getId()),
                createRole(isBuyer, isTaker),
                myIdentity,
                taker,
                maker);
    }

    protected Trade(State state,
                    String id,
                    TradeRole tradeRole,
                    Identity myIdentity,
                    P taker,
                    P maker) {
        super(state);

        this.id = id;
        this.tradeRole = tradeRole;
        this.myIdentity = myIdentity;
        this.taker = taker;
        this.maker = maker;
    }

    protected bisq.trade.protobuf.Trade.Builder getTradeBuilder(boolean serializeForHash) {
        bisq.trade.protobuf.Trade.Builder builder = bisq.trade.protobuf.Trade.newBuilder()
                .setId(id)
                .setTradeRole(tradeRole.toProtoEnum())
                .setMyIdentity(myIdentity.toProto(serializeForHash))
                .setTaker(taker.toProto(serializeForHash))
                .setMaker(maker.toProto(serializeForHash))
                .setState(getState().name());
        Optional.ofNullable(contract.get()).ifPresent(contract -> builder.setContract(contract.toProto(serializeForHash)));
        Optional.ofNullable(getErrorMessage()).ifPresent(builder::setErrorMessage);
        Optional.ofNullable(getErrorStackTrace()).ifPresent(builder::setErrorStackTrace);
        Optional.ofNullable(getPeersErrorMessage()).ifPresent(builder::setPeersErrorMessage);
        Optional.ofNullable(getPeersErrorStackTrace()).ifPresent(builder::setPeersErrorStackTrace);
        return builder;
    }

    public C getContract() {
        return contract.get();
    }

    public void setContract(C contract) {
        this.contract.set(contract);
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage.set(errorMessage);
    }

    public ReadOnlyObservable<String> errorMessageObservable() {
        return errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public void setErrorStackTrace(String peersErrorStacktrace) {
        this.errorStackTrace.set(peersErrorStacktrace);
    }

    public String getErrorStackTrace() {
        return errorStackTrace.get();
    }

    public void setPeersErrorMessage(String errorMessage) {
        this.peersErrorMessage.set(errorMessage);
    }

    public ReadOnlyObservable<String> peersErrorMessageObservable() {
        return peersErrorMessage;
    }

    public String getPeersErrorMessage() {
        return peersErrorMessage.get();
    }

    public void setPeersErrorStackTrace(String peersErrorStackTrace) {
        this.peersErrorStackTrace.set(peersErrorStackTrace);
    }

    public String getPeersErrorStackTrace() {
        return peersErrorStackTrace.get();
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion.set(protocolVersion);
    }

    public String getProtocolVersion() {
        return this.protocolVersion.get();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Delegates
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public T getOffer() {
        checkArgument(contract.get() != null, "Cannot get offer, since contract has not yet been set.");
        return contract.get().getOffer();
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
}
