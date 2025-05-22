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

package bisq.trade.mu_sig.events.seller_as_maker;

import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.handler.MuSigTradeEventHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.grpc.SwapTxSignatureResponse;
import bisq.trade.mu_sig.messages.network.MuSigPaymentReceivedMessage_F;
import bisq.trade.mu_sig.messages.network.vo.PartialSignatures;
import bisq.trade.mu_sig.messages.network.vo.SwapTxSignature;
import bisq.trade.protobuf.SwapTxSignatureRequest;
import com.google.protobuf.ByteString;

public final class MuSigPaymentReceiptConfirmedEventHandler extends MuSigTradeEventHandlerAsMessageSender<MuSigTrade, MuSigPaymentReceiptConfirmedEvent> {
    private SwapTxSignatureResponse mySwapTxSignatureResponse;

    public MuSigPaymentReceiptConfirmedEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void process(MuSigPaymentReceiptConfirmedEvent event) {
        MuSigTradeParty peer = trade.getTaker();
        // We got that from an earlier message
        PartialSignatures peersPartialSignatures = peer.getPeersPartialSignatures().orElseThrow();

        mySwapTxSignatureResponse = SwapTxSignatureResponse.fromProto(musigBlockingStub.signSwapTx(SwapTxSignatureRequest.newBuilder()
                .setTradeId(trade.getId())
                // NOW send the redacted buyer's swapTxInputPartialSignature:
                .setSwapTxInputPeersPartialSignature(ByteString.copyFrom(peersPartialSignatures.getSwapTxInputPartialSignature()))
                .build()));

        muSigTradeService.startCloseTimeout(trade, new MuSigSellersCloseTimeoutEvent());
    }

    @Override
    protected void commit() {
        MuSigTradeParty mySelf = trade.getMaker();
        mySelf.setMySwapTxSignatureResponse(mySwapTxSignatureResponse);
    }

    @Override
    protected void sendMessage() {
        SwapTxSignature swapTxSignature = SwapTxSignature.from(mySwapTxSignatureResponse);
        send(new MuSigPaymentReceivedMessage_F(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                swapTxSignature));
    }

    @Override
    protected void sendLogMessage() {

    }
}
