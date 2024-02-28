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

package bisq.security.pow.equihash;

import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.ProofOfWorkService;
import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Arrays;

// TODO the scaling of the difficulty does not provide the expected results
// Borrowed from: https://github.com/bisq-network/bisq
@Slf4j
public class EquihashProofOfWorkService extends ProofOfWorkService {
    /**
     * Rough cost of two Hashcash iterations compared to solving an Equihash-90-5 puzzle of unit difficulty.
     */
    private static final double DIFFICULTY_SCALE_FACTOR = 3.0e-5;

    public EquihashProofOfWorkService() {
        super();
    }

    @Override
    public ProofOfWork mint(byte[] payload, @Nullable byte[] challenge, double difficulty) {
        double scaledDifficulty = scaledDifficulty(difficulty);
        log.debug("Got scaled & adjusted difficulty: {}", scaledDifficulty);

        long ts = System.currentTimeMillis();
        byte[] seed = getSeed(payload, challenge);
        byte[] solution = new Equihash(90, 5, scaledDifficulty).puzzle(seed).findSolution().serialize();
        long counter = Longs.fromByteArray(Arrays.copyOf(solution, 8));
        long duration = System.currentTimeMillis() - ts;
        var proofOfWork = new ProofOfWork(payload, counter, challenge, difficulty, solution, duration);
        log.debug("Completed minting proofOfWork: {}. {} iterations took {} ms.", proofOfWork, counter, duration);
        return proofOfWork;
    }

    private byte[] getSeed(byte[] payload, @Nullable byte[] challenge) {
        if (challenge == null) {
            return DigestUtil.sha256(payload);
        } else {
            return DigestUtil.sha256(Bytes.concat(payload, challenge));
        }
    }

    @Override
    public byte[] getChallenge(String itemId, String ownerId) {
        String escapedItemId = itemId.replace(" ", "  ");
        String escapedOwnerId = ownerId.replace(" ", "  ");
        String concatenated = escapedItemId + ", " + escapedOwnerId;
        return DigestUtil.sha256(concatenated.getBytes(Charsets.UTF_8));
    }

    @Override
    public boolean verify(ProofOfWork proofOfWork) {
        double scaledDifficulty = scaledDifficulty(proofOfWork.getDifficulty());
        byte[] seed = getSeed(proofOfWork.getPayload(), proofOfWork.getChallenge());
        var puzzle = new Equihash(90, 5, scaledDifficulty).puzzle(seed);
        return puzzle.deserializeSolution(proofOfWork.getSolution()).verify();
    }

    private static double scaledDifficulty(double difficulty) {
        return Equihash.adjustDifficulty(DIFFICULTY_SCALE_FACTOR * difficulty);
    }
}
