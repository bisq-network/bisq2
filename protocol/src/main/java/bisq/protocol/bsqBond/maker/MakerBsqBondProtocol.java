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

package bisq.protocol.bsqBond.maker;


import bisq.contract.TwoPartyContract;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.protocol.bsqBond.BsqBond;
import bisq.protocol.bsqBond.BsqBondProtocol;
import bisq.protocol.bsqBond.taker.TakerCommitmentMessage;
import bisq.protocol.bsqBond.taker.TakerFundsSentMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class MakerBsqBondProtocol extends BsqBondProtocol {
    public MakerBsqBondProtocol(NetworkService networkService,
                                NetworkIdWithKeyPair networkIdWithKeyPair, 
                                TwoPartyContract contract,
                                BsqBond bsqBond) {
        super(networkService, networkIdWithKeyPair, contract, bsqBond);
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof TakerCommitmentMessage takerCommitmentMessage) {
            bsqBond.verifyBondCommitmentMessage(takerCommitmentMessage)
                    .whenComplete((success, t) -> setState(State.COMMITMENT_RECEIVED))
                    .thenCompose(isValid -> contract.getSettlementExecution().sendFunds(contract))
                    .thenCompose(isSent -> networkService.confidentialSendAsync(new MakerFundsSentMessage(),
                            taker.networkId(),
                            networkIdWithKeyPair))
                    .whenComplete((connection1, t) -> setState(State.FUNDS_SENT));
        }
        if (message instanceof TakerFundsSentMessage takerFundsSentMessage) {
            bsqBond.verifyFundsSentMessage(takerFundsSentMessage)
                    .whenComplete((isValid, t) -> {
                        if (isValid) {
                            setState(State.FUNDS_RECEIVED);
                        }
                    });
        }
    }

    public CompletableFuture<Boolean> start() {
        networkService.addMessageListener(this);
        setState(State.START);
        bsqBond.getCommitment(contract)
                .thenCompose(commitment -> networkService.confidentialSendAsync(new MakerCommitmentMessage(commitment),
                        taker.networkId(),
                        networkIdWithKeyPair))
                .whenComplete((success, t) -> setState(State.COMMITMENT_SENT));
        return CompletableFuture.completedFuture(true);
    }
}
