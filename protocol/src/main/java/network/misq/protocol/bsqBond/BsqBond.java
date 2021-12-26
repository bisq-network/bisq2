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

package network.misq.protocol.bsqBond;


import lombok.extern.slf4j.Slf4j;
import network.misq.contract.Contract;
import network.misq.protocol.SecurityProvider;
import network.misq.protocol.bsqBond.messages.CommitmentMessage;
import network.misq.protocol.bsqBond.messages.FundsSentMessage;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class BsqBond implements SecurityProvider {
    public BsqBond() {
    }

    @Override
    public Type getType() {
        return Type.BOND;
    }

    public CompletableFuture<String> getCommitment(Contract contract) {
        return CompletableFuture.completedFuture("commitment");
    }

    public CompletableFuture<Boolean> verifyBondCommitmentMessage(CommitmentMessage commitmentMessage) {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> verifyFundsSentMessage(FundsSentMessage fundsSentMessage) {
        return CompletableFuture.completedFuture(true);
    }
}
