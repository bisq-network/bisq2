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

import bisq.network.identity.NetworkId;
import bisq.trade.TradeParty;
import bisq.trade.mu_sig.messages.grpc.CloseTradeResponse;
import bisq.trade.mu_sig.messages.grpc.DepositPsbt;
import bisq.trade.mu_sig.messages.grpc.NonceSharesMessage;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import bisq.trade.mu_sig.messages.grpc.PubKeySharesResponse;
import bisq.trade.mu_sig.messages.grpc.SwapTxSignatureResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public final class MuSigTradeParty extends TradeParty {
    private Optional<PubKeySharesResponse> pubKeySharesResponse = Optional.empty();
    private Optional<NonceSharesMessage> nonceSharesMessage = Optional.empty();
    private Optional<PartialSignaturesMessage> partialSignaturesMessage = Optional.empty();
    private Optional<DepositPsbt> depositPsbt = Optional.empty();
    private Optional<SwapTxSignatureResponse> swapTxSignatureResponse = Optional.empty();
    private Optional<CloseTradeResponse> closeTradeResponse = Optional.empty();

    public MuSigTradeParty(NetworkId networkId) {
        super(networkId);
    }

    public MuSigTradeParty(NetworkId networkId,
                           Optional<PubKeySharesResponse> pubKeySharesResponse,
                           Optional<NonceSharesMessage> nonceSharesMessage,
                           Optional<PartialSignaturesMessage> partialSignaturesMessage,
                           Optional<DepositPsbt> depositPsbt,
                           Optional<SwapTxSignatureResponse> swapTxSignatureResponse,
                           Optional<CloseTradeResponse> closeTradeResponse) {
        super(networkId);

        this.pubKeySharesResponse = pubKeySharesResponse;
        this.nonceSharesMessage = nonceSharesMessage;
        this.partialSignaturesMessage = partialSignaturesMessage;
        this.depositPsbt = depositPsbt;
        this.swapTxSignatureResponse = swapTxSignatureResponse;
        this.closeTradeResponse = closeTradeResponse;
    }

    @Override
    public bisq.trade.protobuf.TradeParty.Builder getBuilder(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigTradeParty.Builder builder = bisq.trade.protobuf.MuSigTradeParty.newBuilder();
        pubKeySharesResponse.ifPresent(e -> builder.setPubKeySharesResponse(e.toProto(serializeForHash)));
        nonceSharesMessage.ifPresent(e -> builder.setNonceSharesMessage(e.toProto(serializeForHash)));
        partialSignaturesMessage.ifPresent(e -> builder.setPartialSignaturesMessage(e.toProto(serializeForHash)));
        depositPsbt.ifPresent(e -> builder.setDepositPsbt(e.toProto(serializeForHash)));
        swapTxSignatureResponse.ifPresent(e -> builder.setSwapTxSignatureResponse(e.toProto(serializeForHash)));
        closeTradeResponse.ifPresent(e -> builder.setCloseTradeResponse(e.toProto(serializeForHash)));
        return getTradePartyBuilder(serializeForHash).setMuSigTradeParty(builder);
    }

    public static MuSigTradeParty fromProto(bisq.trade.protobuf.TradeParty proto) {
        bisq.trade.protobuf.MuSigTradeParty muSigTradePartyProto = proto.getMuSigTradeParty();
        return new MuSigTradeParty(
                NetworkId.fromProto(proto.getNetworkId()),
                muSigTradePartyProto.hasPubKeySharesResponse()
                        ? Optional.of(PubKeySharesResponse.fromProto(muSigTradePartyProto.getPubKeySharesResponse()))
                        : Optional.empty(),
                muSigTradePartyProto.hasNonceSharesMessage()
                        ? Optional.of(NonceSharesMessage.fromProto(muSigTradePartyProto.getNonceSharesMessage()))
                        : Optional.empty(),
                muSigTradePartyProto.hasPartialSignaturesMessage()
                        ? Optional.of(PartialSignaturesMessage.fromProto(muSigTradePartyProto.getPartialSignaturesMessage()))
                        : Optional.empty(),
                muSigTradePartyProto.hasDepositPsbt()
                        ? Optional.of(DepositPsbt.fromProto(muSigTradePartyProto.getDepositPsbt()))
                        : Optional.empty(),
                muSigTradePartyProto.hasSwapTxSignatureResponse()
                        ? Optional.of(SwapTxSignatureResponse.fromProto(muSigTradePartyProto.getSwapTxSignatureResponse()))
                        : Optional.empty(),
                muSigTradePartyProto.hasCloseTradeResponse()
                        ? Optional.of(CloseTradeResponse.fromProto(muSigTradePartyProto.getCloseTradeResponse()))
                        : Optional.empty()
        );
    }

    public void setPubKeySharesResponse(PubKeySharesResponse pubKeySharesResponse) {
        this.pubKeySharesResponse = Optional.of(pubKeySharesResponse);
    }

    public void setNonceSharesMessage(NonceSharesMessage nonceSharesMessage) {
        this.nonceSharesMessage = Optional.of(nonceSharesMessage);
    }

    public void setPartialSignaturesMessage(PartialSignaturesMessage partialSignaturesMessage) {
        this.partialSignaturesMessage = Optional.of(partialSignaturesMessage);
    }

    public void setDepositPsbt(DepositPsbt depositPsbt) {
        this.depositPsbt = Optional.of(depositPsbt);
    }

    public void setSwapTxSignatureResponse(SwapTxSignatureResponse swapTxSignatureResponse) {
        this.swapTxSignatureResponse = Optional.of(swapTxSignatureResponse);
    }

    public void setCloseTradeResponse(CloseTradeResponse closeTradeResponse) {
        this.closeTradeResponse = Optional.of(closeTradeResponse);
    }
}