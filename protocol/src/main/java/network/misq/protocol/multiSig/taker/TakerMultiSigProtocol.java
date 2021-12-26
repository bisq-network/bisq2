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

package network.misq.protocol.multiSig.taker;


import lombok.extern.slf4j.Slf4j;
import network.misq.contract.AssetTransfer;
import network.misq.contract.TwoPartyContract;
import network.misq.network.NetworkService;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Connection;
import network.misq.protocol.SecurityProvider;
import network.misq.protocol.multiSig.MultiSig;
import network.misq.protocol.multiSig.MultiSigProtocol;
import network.misq.protocol.multiSig.maker.FundsSentMessage;
import network.misq.protocol.multiSig.maker.TxInputsMessage;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class TakerMultiSigProtocol extends MultiSigProtocol implements MultiSig.Listener {

    public TakerMultiSigProtocol(TwoPartyContract contract, NetworkService networkService, SecurityProvider securityProvider) {
        super(contract, networkService, new AssetTransfer.Automatic(), securityProvider);
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof TxInputsMessage) {
            TxInputsMessage txInputsMessage = (TxInputsMessage) message;
            multiSig.verifyTxInputsMessage(txInputsMessage)
                    .whenComplete((txInput, t) -> setState(State.TX_INPUTS_RECEIVED))
                    .thenCompose(multiSig::broadcastDepositTx)
                    .whenComplete((depositTx, t) -> setState(State.DEPOSIT_TX_BROADCAST))
                    .thenCompose(depositTx -> networkService.confidentialSend(new DepositTxBroadcastMessage(depositTx),
                            counterParty.getMakerNetworkId(),
                            null, null))
                    .whenComplete((connection1, t) -> setState(State.DEPOSIT_TX_BROADCAST_MSG_SENT));
        } else if (message instanceof FundsSentMessage) {
            FundsSentMessage fundsSentMessage = (FundsSentMessage) message;
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
                .thenCompose(payoutTx -> networkService.confidentialSend(new PayoutTxBroadcastMessage(payoutTx),
                        counterParty.getMakerNetworkId(),
                        null, null))
                .whenComplete((isValid, t) -> setState(State.PAYOUT_TX_BROADCAST_MSG_SENT));
    }
}
