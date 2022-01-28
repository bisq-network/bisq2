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

package bisq.protocol.prototype.multiSig.taker;


import bisq.protocol.prototype.SettlementExecution;
import bisq.contract.TwoPartyContract;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.protocol.prototype.multiSig.MultiSig;
import bisq.protocol.prototype.multiSig.MultiSigProtocol;
import bisq.protocol.prototype.multiSig.maker.FundsSentMessage;
import bisq.protocol.prototype.multiSig.maker.TxInputsMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class TakerMultiSigProtocol extends MultiSigProtocol implements MultiSig.Listener {
    public TakerMultiSigProtocol(NetworkService networkService,
                                 NetworkIdWithKeyPair networkIdWithKeyPair,
                                 TwoPartyContract contract,
                                 SettlementExecution settlementExecution,
                                 MultiSig multiSig) {
        super(networkService, networkIdWithKeyPair, contract, settlementExecution, multiSig);
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof TxInputsMessage txInputsMessage) {
            multiSig.verifyTxInputsMessage(txInputsMessage)
                    .whenComplete((txInput, t) -> setState(State.TX_INPUTS_RECEIVED))
                    .thenCompose(multiSig::broadcastDepositTx)
                    .whenComplete((depositTx, t) -> setState(State.DEPOSIT_TX_BROADCAST))
                    .thenCompose(depositTx -> networkService.confidentialSendAsync(new DepositTxBroadcastMessage(depositTx),
                            maker.networkId(),
                            networkIdWithKeyPair))
                    .whenComplete((connection1, t) -> setState(State.DEPOSIT_TX_BROADCAST_MSG_SENT));
        } else if (message instanceof FundsSentMessage fundsSentMessage) {
            multiSig.verifyFundsSentMessage(fundsSentMessage)
                    .whenComplete((signature, t) -> {
                        multiSig.setPayoutSignature(signature);
                        setState(State.FUNDS_SENT_MSG_RECEIVED);
                    });
        }
    }

    @Override
    public void onDepositTxConfirmed() {
        setState(State.DEPOSIT_TX_CONFIRMED);
    }

    public CompletableFuture<Boolean> start() {
        networkService.addMessageListener(this);
        multiSig.addListener(this);
        setState(State.START);
        return CompletableFuture.completedFuture(true);
    }

    // Called by user or by altcoin explorer lookup
    public void onFundsReceived() {
        setState(State.FUNDS_RECEIVED);
        multiSig.broadcastPayoutTx()
                .whenComplete((payoutTx, t) -> setState(State.PAYOUT_TX_BROADCAST))
                .thenCompose(payoutTx -> networkService.confidentialSendAsync(new PayoutTxBroadcastMessage(payoutTx),
                        maker.networkId(),
                        networkIdWithKeyPair))
                .whenComplete((isValid, t) -> setState(State.PAYOUT_TX_BROADCAST_MSG_SENT));
    }
}
