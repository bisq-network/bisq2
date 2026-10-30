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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A token of a peer can carry any difficulty, and its string goes into logs and exception messages, so the string of
 * a proof of work must not fail for any difficulty.
 */
class ProofOfWorkTest {
    @Test
    void log2DifficultyIsRoundedForAPositiveDifficulty() {
        assertThat(proofOfWork(1024).getLog2Difficulty()).isEqualTo(10.0);
        assertThat(proofOfWork(3).getLog2Difficulty()).isEqualTo(1.58);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY})
    void toStringDoesNotFailForADifficultyWithoutAFiniteLogarithm(double difficulty) {
        assertThat(Double.isFinite(proofOfWork(difficulty).getLog2Difficulty())).isFalse();
        assertThat(proofOfWork(difficulty).toString()).contains(" = " + difficulty);
    }

    private static ProofOfWork proofOfWork(double difficulty) {
        return new ProofOfWork(new byte[20], 0, null, difficulty, new byte[72], 0);
    }
}
