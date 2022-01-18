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

package bisq.protocol.bsqBond.taker;


import bisq.contract.TwoPartyContract;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.protocol.SettlementExecution;
import bisq.protocol.bsqBond.BsqBond;
import bisq.protocol.bsqBond.BsqBondProtocol;
import bisq.protocol.bsqBond.maker.MakerCommitmentMessage;
import bisq.protocol.bsqBond.maker.MakerFundsSentMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class TakerBsqBondProtocol extends BsqBondProtocol {

    public TakerBsqBondProtocol(NetworkService networkService,
                                NetworkIdWithKeyPair networkIdWithKeyPair,
                                TwoPartyContract contract,
                                SettlementExecution settlementExecution,
                                BsqBond bsqBond) {
        super(networkService, networkIdWithKeyPair, contract, settlementExecution, bsqBond);
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof MakerCommitmentMessage makerCommitmentMessage) {
            bsqBond.verifyBondCommitmentMessage(makerCommitmentMessage)
                    .whenComplete((success, t) -> setState(State.COMMITMENT_RECEIVED))
                    .thenCompose(isValid -> bsqBond.getCommitment(contract))
                    .thenCompose(commitment -> networkService.confidentialSendAsync(new TakerCommitmentMessage(commitment),
                            maker.networkId(),
                            networkIdWithKeyPair))
                    .whenComplete((success, t) -> setState(State.COMMITMENT_SENT));
        }
        if (message instanceof MakerFundsSentMessage makerFundsSentMessage) {
            bsqBond.verifyFundsSentMessage(makerFundsSentMessage)
                    .whenComplete((success, t) -> setState(State.FUNDS_RECEIVED))
                    .thenCompose(isValid -> settlementExecution.sendFunds(contract))
                    .thenCompose(isSent -> networkService.confidentialSendAsync(new TakerFundsSentMessage(),
                            maker.networkId(),
                            networkIdWithKeyPair))
                    .whenComplete((success, t) -> setState(State.FUNDS_SENT));
        }
    }


    public CompletableFuture<Boolean> start() {
        networkService.addMessageListener(this);
        setState(State.START);
        return CompletableFuture.completedFuture(true);
    }
}
