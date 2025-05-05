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

package bisq.trade.mu_sig.messages.network;

import bisq.contract.ContractSignatureData;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.trade.mu_sig.messages.grpc.PubKeySharesResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class MuSigSetupTradeMessage_B extends MuSigTradeMessage {
    public final static int MAX_LENGTH = 1000;
    private final MuSigContract contract;
    private final ContractSignatureData contractSignatureData;
    private final PubKeySharesResponse pubKeySharesResponse;

    public MuSigSetupTradeMessage_B(String id,
                                    String tradeId,
                                    String protocolVersion,
                                    NetworkId sender,
                                    NetworkId receiver,
                                    MuSigContract contract,
                                    ContractSignatureData contractSignatureData,
                                    PubKeySharesResponse pubKeySharesResponse) {
        super(id, tradeId, protocolVersion, sender, receiver);
        this.contract = contract;
        this.contractSignatureData = contractSignatureData;
        this.pubKeySharesResponse = pubKeySharesResponse;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeMessage.newBuilder()
                .setMuSigSetupTradeMessageB(toMuSigSetupTradeMessage_BProto(serializeForHash));
    }

    private bisq.trade.protobuf.MuSigSetupTradeMessage_B toMuSigSetupTradeMessage_BProto(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigSetupTradeMessage_B.Builder builder = getMuSigSetupTradeMessage_B(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.MuSigSetupTradeMessage_B.Builder getMuSigSetupTradeMessage_B(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigSetupTradeMessage_B.newBuilder();
    }

    public static MuSigSetupTradeMessage_B fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.MuSigSetupTradeMessage_B muSigMessageProto = proto.getMuSigTradeMessage().getMuSigSetupTradeMessageB();
        return new MuSigSetupTradeMessage_B(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                MuSigContract.fromProto(muSigMessageProto.getContract()),
                ContractSignatureData.fromProto(muSigMessageProto.getContractSignatureData()),
                PubKeySharesResponse.fromProto(muSigMessageProto.getPubKeySharesResponse()));
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
