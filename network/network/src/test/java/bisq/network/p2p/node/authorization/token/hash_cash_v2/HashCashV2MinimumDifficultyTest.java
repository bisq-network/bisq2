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

package bisq.network.p2p.node.authorization.token.hash_cash_v2;

import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.keep_alive.Ping;
import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashCashV2MinimumDifficultyTest {
    private static final String RECEIVER_ADDRESS = "receiver.onion:1000";
    private static final NetworkLoad LOW_LOAD = new NetworkLoad(0.01, 1);
    private static final NetworkLoad HIGH_LOAD = new NetworkLoad(1, 1000);

    private final HashCashProofOfWorkService proofOfWorkService = new HashCashProofOfWorkService();
    private final HashCashV2TokenService tokenService = new HashCashV2TokenService(proofOfWorkService);

    @ParameterizedTest
    @MethodSource("belowMinimumDifficulties")
    void loadToleranceCannotAuthorizeAProofBelowTheMinimum(double difficulty, boolean previousLoadAvailable) {
        Ping message = new Ping(1);
        HashCashV2Token token = mintToken(message, difficulty);
        assertTrue(proofOfWorkService.verify(token.getProofOfWork()));

        assertFalse(tokenService.isAuthorized(message, token,
                previousLoadAvailable ? HIGH_LOAD : LOW_LOAD,
                previousLoadAvailable ? Optional.of(LOW_LOAD) : Optional.empty(),
                ConcurrentHashMap.newKeySet(), RECEIVER_ADDRESS));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void proofAtTheMinimumRemainsValidAtLowLoadOrThePreviousLowLoad(boolean previousLoadAvailable) {
        Ping message = new Ping(1);
        HashCashV2Token token = mintToken(message, HashCashV2TokenService.MIN_DIFFICULTY);

        assertTrue(tokenService.isAuthorized(message, token,
                previousLoadAvailable ? HIGH_LOAD : LOW_LOAD,
                previousLoadAvailable ? Optional.of(LOW_LOAD) : Optional.empty(),
                ConcurrentHashMap.newKeySet(), RECEIVER_ADDRESS));
    }

    @ParameterizedTest
    @MethodSource("nonFiniteDifficulties")
    void nonFiniteDifficultiesAreRejectedWithoutThrowing(double difficulty) {
        Ping message = new Ping(1);
        ProofOfWork valid = mintToken(message, HashCashV2TokenService.MIN_DIFFICULTY).getProofOfWork();
        ProofOfWork malformed = new ProofOfWork(valid.getPayload(), valid.getCounter(), valid.getChallenge(),
                difficulty, valid.getSolution(), valid.getDuration());

        assertFalse(tokenService.isAuthorized(message, new HashCashV2Token(malformed, 0), LOW_LOAD,
                Optional.empty(), ConcurrentHashMap.newKeySet(), RECEIVER_ADDRESS));
    }

    private HashCashV2Token mintToken(Ping message, double difficulty) {
        ProofOfWork proofOfWork = proofOfWorkService.mint(DigestUtil.hash(message.serializeForHash()),
                HashCashV2TokenService.getChallenge(RECEIVER_ADDRESS, 0), difficulty);
        return new HashCashV2Token(proofOfWork, 0);
    }

    private static Stream<Arguments> belowMinimumDifficulties() {
        return Stream.of(0.0, 1.0, Math.nextDown((double) HashCashV2TokenService.MIN_DIFFICULTY))
                .flatMap(difficulty -> Stream.of(Arguments.of(difficulty, false), Arguments.of(difficulty, true)));
    }

    private static Stream<Double> nonFiniteDifficulties() {
        return Stream.of(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }
}
