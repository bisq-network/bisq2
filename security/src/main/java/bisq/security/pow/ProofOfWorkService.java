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

package bisq.security.pow;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

// Borrowed from: https://github.com/bisq-network/bisq
@Slf4j
public abstract class ProofOfWorkService {
    public final static int MINT_NYM_DIFFICULTY = 65536;  // Math.pow(2, 16) = 65536;


    public ProofOfWorkService() {
    }

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    public abstract CompletableFuture<ProofOfWork> mint(byte[] payload, byte[] challenge, double difficulty);

    public abstract boolean verify(ProofOfWork proofOfWork);

    public byte[] asUtf8Bytes(String itemId) {
        return itemId.getBytes(StandardCharsets.UTF_8);
    }

    public abstract byte[] getChallenge(String itemId, String ownerId);

    public CompletableFuture<ProofOfWork> mint(String itemId, String ownerId, double difficulty) {
        return mint(asUtf8Bytes(itemId), getChallenge(itemId, ownerId), difficulty);
    }

    public CompletableFuture<ProofOfWork> mintNymProofOfWork(byte[] pubKeyHash) {
        return mintNymProofOfWork(pubKeyHash, MINT_NYM_DIFFICULTY);
    }

    public CompletableFuture<ProofOfWork> mintNymProofOfWork(byte[] pubKeyHash, double nymDifficulty) {
        return mint(pubKeyHash, null, nymDifficulty);
    }

    public boolean verify(ProofOfWork proofOfWork,
                          String itemId,
                          String ownerId,
                          double controlDifficulty) {
        byte[] controlChallenge = getChallenge(itemId, ownerId);
        return Arrays.equals(proofOfWork.getChallenge(), controlChallenge) &&
                proofOfWork.getDifficulty() >= controlDifficulty &&
                verify(proofOfWork);
    }
}
