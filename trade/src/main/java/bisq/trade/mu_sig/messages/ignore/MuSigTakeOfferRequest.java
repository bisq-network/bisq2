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

package bisq.trade.mu_sig.messages.ignore;

import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_musig.BisqMuSigContract;
import bisq.network.identity.NetworkId;
import bisq.trade.mu_sig.messages.MuSigTradeMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class MuSigTakeOfferRequest extends MuSigTradeMessage {
    private final BisqMuSigContract bisqMuSigContract;
    private final ContractSignatureData contractSignatureData;

    public MuSigTakeOfferRequest(String id,
                                 String tradeId,
                                 String protocolVersion,
                                 NetworkId sender,
                                 NetworkId receiver,
                                 BisqMuSigContract bisqMuSigContract,
                                 ContractSignatureData contractSignatureData) {
        super(id, tradeId, protocolVersion, sender, receiver);

        this.bisqMuSigContract = bisqMuSigContract;
        this.contractSignatureData = contractSignatureData;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    protected bisq.trade.protobuf.BisqEasyTradeMessage.Builder getBisqEasyTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.BisqEasyTradeMessage.newBuilder()
                .setBisqEasyAccountDataMessage(toBisqEasyAccountDataMessageProto(serializeForHash));
    }

    private bisq.trade.protobuf.BisqEasyAccountDataMessage toBisqEasyAccountDataMessageProto(boolean serializeForHash) {
        bisq.trade.protobuf.BisqEasyAccountDataMessage.Builder builder = getBisqEasyAccountDataMessageBuilder(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.BisqEasyAccountDataMessage.Builder getBisqEasyAccountDataMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.BisqEasyAccountDataMessage.newBuilder();
    }

    /*public static BisqMuSigTakeOfferRequest fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.BisqMuSigTakeOfferRequest bisqMuSigTakeOfferRequest = proto.getBisqMuSigTradeMessage().getBisqMuSigTakeOfferRequest();
        return new BisqMuSigTakeOfferRequest(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                BisqMuSigContract.fromProto(bisqMuSigTakeOfferRequest.getBisqMuSigContract()),
                ContractSignatureData.fromProto(bisqMuSigTakeOfferRequest.getContractSignatureData()));
    }*/

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
