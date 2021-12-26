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

package network.misq.protocol.multiSig;

import network.misq.contract.AssetTransfer;
import network.misq.contract.TwoPartyContract;
import network.misq.network.NetworkService;
import network.misq.network.p2p.node.Node;
import network.misq.protocol.Protocol;
import network.misq.protocol.SecurityProvider;
import network.misq.protocol.TwoPartyProtocol;

/**
 * Mock protocol for simulating the a basic 2of2 Multisig protocol (MAD). Maker is BTC buyer, Taker is seller. There
 * might be differences for buyer/seller roles which would lead to 4 protocol variations, but we keep it simple and
 * consider there are only 2 roles.
 * <ol>
 *   <li value="1">Maker sends tx inputs.
 *   <li value="3">Maker receives signed 2of MS tx from Taker, signs it and broadcasts it. Wait for confirmation.
 *   <li value="4">After Tx is confirmed she sends funds to Taker and sends Taker a message including her signature for
 *   the payout tx.
 *   <li value="6">After Maker has received Takers message and sees the payout tx in the network she has completed.
 * </ol>
 * <p>
 * Taker awaits Maker commitment.
 * <ol>
 *   <li value="2">Taker receives tx inputs of Maker and creates 2of MS tx, signs it and send it back to Maker. Wait for
 *   confirmation once Maker has broadcast tx.
 *   <li value="5">After Taker has received Maker message, checks if he has received the funds and if so he signs the
 *   payout tx and broadcasts it and sends Maker a message that the payout tx is broadcast.
 * </ol>
 */
public abstract class MultiSigProtocol extends TwoPartyProtocol implements Node.Listener {

    public enum State implements Protocol.State {
        START,
        TX_INPUTS_SENT,
        TX_INPUTS_RECEIVED,
        DEPOSIT_TX_BROADCAST,
        DEPOSIT_TX_BROADCAST_MSG_SENT,
        DEPOSIT_TX_BROADCAST_MSG_RECEIVED,
        DEPOSIT_TX_CONFIRMED,
        START_MANUAL_PAYMENT,
        MANUAL_PAYMENT_STARTED,
        FUNDS_SENT,
        FUNDS_SENT_MSG_SENT,
        FUNDS_SENT_MSG_RECEIVED,
        FUNDS_RECEIVED,
        PAYOUT_TX_BROADCAST,
        PAYOUT_TX_BROADCAST_MSG_SENT, // Taker completed
        PAYOUT_TX_BROADCAST_MSG_RECEIVED,
        PAYOUT_TX_VISIBLE_IN_MEM_POOL // Maker completed
    }

    protected final AssetTransfer assetTransfer;
    protected final MultiSig multiSig;

    public MultiSigProtocol(TwoPartyContract contract, NetworkService networkService, AssetTransfer transfer, SecurityProvider securityProvider) {
        super(contract, networkService);
        this.assetTransfer = transfer;

        this.multiSig = (MultiSig) securityProvider;

        if (assetTransfer instanceof AssetTransfer.Manual) {
            ((AssetTransfer.Manual) assetTransfer).addListener(this::onStartManualPayment);
            addListener(state -> {
                if (state == State.MANUAL_PAYMENT_STARTED) {
                    ((AssetTransfer.Manual) assetTransfer).onManualPaymentStarted();
                }
            });
        }
    }

    private void onStartManualPayment() {
        setState(State.START_MANUAL_PAYMENT);
    }

    public void onManualPaymentStarted() {
        setState(State.MANUAL_PAYMENT_STARTED);
    }
}
