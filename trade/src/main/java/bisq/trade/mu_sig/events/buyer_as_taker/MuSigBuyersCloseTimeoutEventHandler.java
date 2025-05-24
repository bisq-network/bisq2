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

package bisq.trade.mu_sig.events.buyer_as_taker;

import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.handler.MuSigTradeEventHandler;
import bisq.trade.mu_sig.messages.grpc.CloseTradeResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MuSigBuyersCloseTimeoutEventHandler extends MuSigTradeEventHandler<MuSigTrade, MuSigBuyersCloseTimeoutEvent> {
    private CloseTradeResponse myCloseTradeResponse;

    public MuSigBuyersCloseTimeoutEventHandler(ServiceProvider serviceProvider, MuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void process(MuSigBuyersCloseTimeoutEvent event) {
        tradeService.stopCloseTimeout(trade);

        // ClosureType.UNCOOPERATIVE
        // Buyer never got Message F from seller -- picks up Swap Tx from bitcoin network instead.
        // *** BUYER CLOSES TRADE ***
        // TODO get swap tx from bitcoin network

        log.error("Listening for swap tx from bitcoin network is not implemented yet.");

        //  Take from simulated storage of swapTx in blockchain
        /*  Path path = PlatformUtils.getUserDataDir().resolve("swapTx_" + trade.getId());
        try {
            byte[] swapTx = FileUtils.read(path.toString());
            CloseTradeRequest closeTradeRequest = CloseTradeRequest.newBuilder()
                    .setTradeId(trade.getId())
                    .setSwapTx(ByteString.copyFrom(swapTx))
                    .build();
            myCloseTradeResponse = CloseTradeResponse.fromProto(musigBlockingStub.closeTrade(closeTradeRequest));
        } catch (IOException e) {
            log.error("");
            throw new RuntimeException(e);
        }*/
    }

    @Override
    protected void commit() {
        trade.getMyself().setMyCloseTradeResponse(myCloseTradeResponse);
    }

    @Override
    protected void sendLogMessage() {
        sendLogMessage("Buyer did not receive peers swapTxSignature and the timeout got triggered.\n" +
                "Buyer found the sellers swap tx on the blockchain and used it to force-close the trade.");
    }
}
