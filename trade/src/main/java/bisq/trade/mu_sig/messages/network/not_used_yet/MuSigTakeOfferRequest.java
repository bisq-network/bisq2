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

package bisq.trade.mu_sig.messages.network.not_used_yet;

import bisq.contract.ContractSignatureData;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.trade.mu_sig.messages.network.MuSigTradeMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class MuSigTakeOfferRequest extends MuSigTradeMessage {
    private final MuSigContract MuSigContract;
    private final ContractSignatureData contractSignatureData;

    public MuSigTakeOfferRequest(String id,
                                 String tradeId,
                                 String protocolVersion,
                                 NetworkId sender,
                                 NetworkId receiver,
                                 MuSigContract MuSigContract,
                                 ContractSignatureData contractSignatureData) {
        super(id, tradeId, protocolVersion, sender, receiver);

        this.MuSigContract = MuSigContract;
        this.contractSignatureData = contractSignatureData;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeMessage.newBuilder()
                .setMuSigTakeOfferRequest(toMuSigTakeOfferRequestProto(serializeForHash));
    }

    private bisq.trade.protobuf.MuSigTakeOfferRequest toMuSigTakeOfferRequestProto(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigTakeOfferRequest.Builder builder = getMuSigTakeOfferRequest(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.MuSigTakeOfferRequest.Builder getMuSigTakeOfferRequest(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTakeOfferRequest.newBuilder();
    }

    public static MuSigTakeOfferRequest fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.MuSigTakeOfferRequest muSigMessageProto = proto.getMuSigTradeMessage().getMuSigTakeOfferRequest();
        return new MuSigTakeOfferRequest(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                bisq.contract.mu_sig.MuSigContract.fromProto(muSigMessageProto.getContract()),
                ContractSignatureData.fromProto(muSigMessageProto.getContractSignatureData()));
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
