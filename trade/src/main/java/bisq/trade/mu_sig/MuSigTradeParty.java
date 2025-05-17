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
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public final class MuSigTradeParty extends TradeParty {
    private PubKeySharesResponse pubKeySharesResponse;
    private NonceSharesMessage nonceSharesMessage;
    private PartialSignaturesMessage partialSignaturesMessage;
    private DepositPsbt depositPsbt;
    private SwapTxSignatureResponse swapTxSignatureResponse;
    private CloseTradeResponse closeTradeResponse;

    public MuSigTradeParty(NetworkId networkId) {
        super(networkId);
    }

    public MuSigTradeParty(NetworkId networkId,
                           PubKeySharesResponse pubKeySharesResponse,
                           NonceSharesMessage nonceSharesMessage,
                           PartialSignaturesMessage partialSignaturesMessage,
                           DepositPsbt depositPsbt,
                           SwapTxSignatureResponse swapTxSignatureResponse,
                           CloseTradeResponse closeTradeResponse) {
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
        bisq.trade.protobuf.MuSigTradeParty.Builder builder = bisq.trade.protobuf.MuSigTradeParty.newBuilder()
                .setPubKeySharesResponse(pubKeySharesResponse.toProto(serializeForHash))
                .setNonceSharesMessage(nonceSharesMessage.toProto(serializeForHash))
                .setPartialSignaturesMessage(partialSignaturesMessage.toProto(serializeForHash))
                .setDepositPsbt(depositPsbt.toProto(serializeForHash))
                .setSwapTxSignatureResponse(swapTxSignatureResponse.toProto(serializeForHash))
                .setCloseTradeResponse(closeTradeResponse.toProto(serializeForHash));
        return getTradePartyBuilder(serializeForHash).setMuSigTradeParty(builder);
    }

    public static MuSigTradeParty fromProto(bisq.trade.protobuf.TradeParty proto) {
        bisq.trade.protobuf.MuSigTradeParty muSigTradePartyProto = proto.getMuSigTradeParty();
        return new MuSigTradeParty(
                NetworkId.fromProto(proto.getNetworkId()),
                PubKeySharesResponse.fromProto(muSigTradePartyProto.getPubKeySharesResponse()),
                NonceSharesMessage.fromProto(muSigTradePartyProto.getNonceSharesMessage()),
                PartialSignaturesMessage.fromProto(muSigTradePartyProto.getPartialSignaturesMessage()),
                DepositPsbt.fromProto(muSigTradePartyProto.getDepositPsbt()),
                SwapTxSignatureResponse.fromProto(muSigTradePartyProto.getSwapTxSignatureResponse()),
                CloseTradeResponse.fromProto(muSigTradePartyProto.getCloseTradeResponse())
        );
    }
}