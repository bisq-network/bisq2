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

import bisq.common.data.ByteArray;
import bisq.network.identity.NetworkId;
import bisq.trade.TradeParty;
import bisq.trade.mu_sig.messages.grpc.CloseTradeResponse;
import bisq.trade.mu_sig.messages.grpc.DepositPsbt;
import bisq.trade.mu_sig.messages.grpc.NonceSharesMessage;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import bisq.trade.mu_sig.messages.grpc.PubKeySharesResponse;
import bisq.trade.mu_sig.messages.grpc.SwapTxSignatureResponse;
import bisq.trade.mu_sig.messages.network.mu_sig_data.NonceShares;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PartialSignatures;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PubKeyShares;
import bisq.trade.mu_sig.messages.network.mu_sig_data.RedactedPartialSignatures;
import bisq.trade.mu_sig.messages.network.mu_sig_data.SwapTxSignature;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public final class MuSigTradeParty extends TradeParty {
    private Optional<PubKeySharesResponse> myPubKeySharesResponse = Optional.empty();
    private Optional<PubKeyShares> peersPubKeyShares = Optional.empty();
    private Optional<NonceSharesMessage> myNonceSharesMessage = Optional.empty();
    private Optional<NonceShares> peersNonceShares = Optional.empty();
    private Optional<PartialSignaturesMessage> myPartialSignaturesMessage = Optional.empty();
    private Optional<PartialSignatures> peersPartialSignatures = Optional.empty();
    private Optional<RedactedPartialSignatures> peersRedactedPartialSignatures = Optional.empty();
    private Optional<DepositPsbt> myDepositPsbt = Optional.empty();
    private Optional<SwapTxSignatureResponse> mySwapTxSignatureResponse = Optional.empty();
    private Optional<SwapTxSignature> peersSwapTxSignature = Optional.empty();
    private Optional<CloseTradeResponse> myCloseTradeResponse = Optional.empty();
    private Optional<ByteArray> peersOutputPrvKeyShare = Optional.empty();

    public MuSigTradeParty(NetworkId networkId) {
        super(networkId);
    }

    public MuSigTradeParty(NetworkId networkId,
                           Optional<PubKeySharesResponse> myPubKeySharesResponse,
                           Optional<PubKeyShares> peersPubKeyShares,
                           Optional<NonceSharesMessage> myNonceSharesMessage,
                           Optional<NonceShares> peersNonceShares,
                           Optional<PartialSignaturesMessage> myPartialSignaturesMessage,
                           Optional<PartialSignatures> peersPartialSignatures,
                           Optional<RedactedPartialSignatures> peersRedactedPartialSignatures,
                           Optional<DepositPsbt> myDepositPsbt,
                           Optional<SwapTxSignatureResponse> mySwapTxSignatureResponse,
                           Optional<SwapTxSignature> peersSwapTxSignature,
                           Optional<CloseTradeResponse> myCloseTradeResponse,
                           Optional<ByteArray> peersOutputPrvKeyShare) {
        super(networkId);

        this.myPubKeySharesResponse = myPubKeySharesResponse;
        this.peersPubKeyShares = peersPubKeyShares;
        this.myNonceSharesMessage = myNonceSharesMessage;
        this.peersNonceShares = peersNonceShares;
        this.myPartialSignaturesMessage = myPartialSignaturesMessage;
        this.peersPartialSignatures = peersPartialSignatures;
        this.peersRedactedPartialSignatures = peersRedactedPartialSignatures;
        this.myDepositPsbt = myDepositPsbt;
        this.mySwapTxSignatureResponse = mySwapTxSignatureResponse;
        this.peersSwapTxSignature = peersSwapTxSignature;
        this.myCloseTradeResponse = myCloseTradeResponse;
        this.peersOutputPrvKeyShare = peersOutputPrvKeyShare;
    }

    @Override
    public bisq.trade.protobuf.TradeParty.Builder getBuilder(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigTradeParty.Builder builder = bisq.trade.protobuf.MuSigTradeParty.newBuilder();
        myPubKeySharesResponse.ifPresent(e -> builder.setMyPubKeySharesResponse(e.toProto(serializeForHash)));
        peersPubKeyShares.ifPresent(e -> builder.setPeersPubKeyShares(e.toProto(serializeForHash)));
        myNonceSharesMessage.ifPresent(e -> builder.setMyNonceSharesMessage(e.toProto(serializeForHash)));
        peersNonceShares.ifPresent(e -> builder.setPeersNonceShares(e.toProto(serializeForHash)));
        myPartialSignaturesMessage.ifPresent(e -> builder.setMyPartialSignaturesMessage(e.toProto(serializeForHash)));
        peersPartialSignatures.ifPresent(e -> builder.setPeersPartialSignatures(e.toProto(serializeForHash)));
        peersRedactedPartialSignatures.ifPresent(e -> builder.setPeersRedactedPartialSignatures(e.toProto(serializeForHash)));
        myDepositPsbt.ifPresent(e -> builder.setMyDepositPsbt(e.toProto(serializeForHash)));
        mySwapTxSignatureResponse.ifPresent(e -> builder.setMySwapTxSignatureResponse(e.toProto(serializeForHash)));
        peersSwapTxSignature.ifPresent(e -> builder.setPeersSwapTxSignature(e.toProto(serializeForHash)));
        myCloseTradeResponse.ifPresent(e -> builder.setMyCloseTradeResponse(e.toProto(serializeForHash)));
        peersOutputPrvKeyShare.ifPresent(e -> builder.setPeersOutputPrvKeyShare(e.toProto(serializeForHash)));
        return getTradePartyBuilder(serializeForHash).setMuSigTradeParty(builder);
    }

    public static MuSigTradeParty fromProto(bisq.trade.protobuf.TradeParty proto) {
        bisq.trade.protobuf.MuSigTradeParty muSigTradePartyProto = proto.getMuSigTradeParty();
        return new MuSigTradeParty(
                NetworkId.fromProto(proto.getNetworkId()),
                muSigTradePartyProto.hasMyPubKeySharesResponse()
                        ? Optional.of(PubKeySharesResponse.fromProto(muSigTradePartyProto.getMyPubKeySharesResponse()))
                        : Optional.empty(),
                muSigTradePartyProto.hasPeersPubKeyShares()
                        ? Optional.of(PubKeyShares.fromProto(muSigTradePartyProto.getPeersPubKeyShares()))
                        : Optional.empty(),
                muSigTradePartyProto.hasMyNonceSharesMessage()
                        ? Optional.of(NonceSharesMessage.fromProto(muSigTradePartyProto.getMyNonceSharesMessage()))
                        : Optional.empty(),
                muSigTradePartyProto.hasPeersNonceShares()
                        ? Optional.of(NonceShares.fromProto(muSigTradePartyProto.getPeersNonceShares()))
                        : Optional.empty(),
                muSigTradePartyProto.hasMyPartialSignaturesMessage()
                        ? Optional.of(PartialSignaturesMessage.fromProto(muSigTradePartyProto.getMyPartialSignaturesMessage()))
                        : Optional.empty(),
                muSigTradePartyProto.hasPeersPartialSignatures()
                        ? Optional.of(PartialSignatures.fromProto(muSigTradePartyProto.getPeersPartialSignatures()))
                        : Optional.empty(),
                muSigTradePartyProto.hasPeersRedactedPartialSignatures()
                        ? Optional.of(RedactedPartialSignatures.fromProto(muSigTradePartyProto.getPeersRedactedPartialSignatures()))
                        : Optional.empty(),
                muSigTradePartyProto.hasMyDepositPsbt()
                        ? Optional.of(DepositPsbt.fromProto(muSigTradePartyProto.getMyDepositPsbt()))
                        : Optional.empty(),
                muSigTradePartyProto.hasMySwapTxSignatureResponse()
                        ? Optional.of(SwapTxSignatureResponse.fromProto(muSigTradePartyProto.getMySwapTxSignatureResponse()))
                        : Optional.empty(),
                muSigTradePartyProto.hasPeersSwapTxSignature()
                        ? Optional.of(SwapTxSignature.fromProto(muSigTradePartyProto.getPeersSwapTxSignature()))
                        : Optional.empty(),
                muSigTradePartyProto.hasMyCloseTradeResponse()
                        ? Optional.of(CloseTradeResponse.fromProto(muSigTradePartyProto.getMyCloseTradeResponse()))
                        : Optional.empty(),
                muSigTradePartyProto.hasPeersOutputPrvKeyShare()
                        ? Optional.of(ByteArray.fromProto(muSigTradePartyProto.getPeersOutputPrvKeyShare()))
                        : Optional.empty()
        );
    }

    public void setMyPubKeySharesResponse(PubKeySharesResponse myPubKeySharesResponse) {
        this.myPubKeySharesResponse = Optional.of(myPubKeySharesResponse);
    }

    public void setPeersPubKeyShares(PubKeyShares peersPubKeyShares) {
        this.peersPubKeyShares = Optional.of(peersPubKeyShares);
    }

    public void setMyNonceSharesMessage(NonceSharesMessage myNonceSharesMessage) {
        this.myNonceSharesMessage = Optional.of(myNonceSharesMessage);
    }

    public void setPeersNonceShares(NonceShares peersNonceShares) {
        this.peersNonceShares = Optional.of(peersNonceShares);
    }

    public void setMyPartialSignaturesMessage(PartialSignaturesMessage myPartialSignaturesMessage) {
        this.myPartialSignaturesMessage = Optional.of(myPartialSignaturesMessage);
    }

    public void setPeersPartialSignatures(PartialSignatures peersPartialSignatures) {
        this.peersPartialSignatures = Optional.of(peersPartialSignatures);
    }

    public void setPeersRedactedPartialSignatures(RedactedPartialSignatures peersRedactedPartialSignatures) {
        this.peersRedactedPartialSignatures = Optional.of(peersRedactedPartialSignatures);
    }

    public void setMyDepositPsbt(DepositPsbt myDepositPsbt) {
        this.myDepositPsbt = Optional.of(myDepositPsbt);
    }

    public void setMySwapTxSignatureResponse(SwapTxSignatureResponse mySwapTxSignatureResponse) {
        this.mySwapTxSignatureResponse = Optional.of(mySwapTxSignatureResponse);
    }

    public void setPeersSwapTxSignature(SwapTxSignature peersSwapTxSignature) {
        this.peersSwapTxSignature = Optional.of(peersSwapTxSignature);
    }

    public void setMyCloseTradeResponse(CloseTradeResponse myCloseTradeResponse) {
        this.myCloseTradeResponse = Optional.of(myCloseTradeResponse);
    }

    public void setPeersOutputPrvKeyShare(ByteArray peersOutputPrvKeyShare) {
        this.peersOutputPrvKeyShare = Optional.of(peersOutputPrvKeyShare);
    }
}