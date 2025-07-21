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

package bisq.trade.mu_sig.events.seller;

import bisq.common.util.StringUtils;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.handler.MuSigTradeEventHandlerAsMessageSender;
import bisq.trade.mu_sig.messages.grpc.SwapTxSignatureResponse;
import bisq.trade.mu_sig.messages.network.PaymentReceivedMessage_F;
import bisq.trade.mu_sig.messages.network.mu_sig_data.SwapTxSignature;
import bisq.trade.protobuf.SwapTxSignatureRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PaymentReceiptConfirmedEventHandler extends MuSigTradeEventHandlerAsMessageSender<MuSigTrade, PaymentReceiptConfirmedEvent> {
    private SwapTxSignatureResponse mySwapTxSignatureResponse;

    public PaymentReceiptConfirmedEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void process(PaymentReceiptConfirmedEvent event) {
        mySwapTxSignatureResponse = SwapTxSignatureResponse.fromProto(blockingStub.signSwapTx(SwapTxSignatureRequest.newBuilder()
                .setTradeId(trade.getId())
                .setSellerReadyToRelease(true)
                .build()));

        tradeService.startCloseTradeTimeout(trade, new SellersCloseTradeTimeoutEvent());
    }

    @Override
    protected void commit() {
        trade.getMyself().setMySwapTxSignatureResponse(mySwapTxSignatureResponse);
    }

    @Override
    protected void sendMessage() {
        SwapTxSignature swapTxSignature = SwapTxSignature.from(mySwapTxSignatureResponse);

        // TODO simulate storage of swapTx in blockchain
      /*  byte[] swapTx = mySwapTxSignatureResponse.getSwapTx();
        Path path = PlatformUtils.getUserDataDir().resolve("swapTx_" + trade.getId());
        try {
            FileUtils.write(path.toString(), swapTx);
        } catch (IOException e) {
            throw new MuSigProtocolException(e);
        }*/

        send(new PaymentReceivedMessage_F(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyIdentity().getNetworkId(),
                trade.getPeer().getNetworkId(),
                swapTxSignature));
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Seller confirmed payment receipt.\n" +
                "Seller sends swapTxSignature to buyer.");
    }
}
