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

import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.keep_alive.Ping;
import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The proof of work of a HASH_CASH_V2 token binds the token to the 20 byte hash of serializeForHash() of the message
 * (see docs/specifications/network/message-framing.md). The receiver accepts no other payload.
 */
class HashCashV2TokenServiceTest {
    private static final String RECEIVER_ADDRESS = "receiver.onion:1000";
    private static final NetworkLoad NETWORK_LOAD = new NetworkLoad(0.5, 1);

    private final HashCashProofOfWorkService proofOfWorkService = new HashCashProofOfWorkService();
    private final HashCashV2TokenService tokenService = new HashCashV2TokenService(proofOfWorkService);

    @Test
    void tokenWithTheHashOfTheMessageIsAuthorized() {
        Ping message = new Ping(1);
        HashCashV2Token token = tokenService.createToken(message, NETWORK_LOAD, RECEIVER_ADDRESS, 0);

        assertArrayEquals(DigestUtil.hash(message.serializeForHash()), token.getProofOfWork().getPayload());
        assertTrue(isAuthorized(message, token));
    }

    // The proof of work of HASH_CASH contains the message itself. A HASH_CASH_V2 token with such a payload is rejected,
    // although its proof of work, its challenge and its difficulty are valid.
    @Test
    void tokenWithTheUnhashedMessageAsPayloadIsNotAuthorized() {
        Ping message = new Ping(1);
        ProofOfWork validProofOfWork = tokenService.createToken(message, NETWORK_LOAD, RECEIVER_ADDRESS, 0)
                .getProofOfWork();
        ProofOfWork proofOfWork = proofOfWorkService.mint(message.serialize(),
                validProofOfWork.getChallenge(),
                validProofOfWork.getDifficulty());
        assertTrue(proofOfWorkService.verify(proofOfWork));

        assertFalse(isAuthorized(message, new HashCashV2Token(proofOfWork, 0)));
    }

    private boolean isAuthorized(EnvelopePayloadMessage message, HashCashV2Token token) {
        return tokenService.isAuthorized(message,
                token,
                NETWORK_LOAD,
                Optional.empty(),
                "connectionId",
                RECEIVER_ADDRESS);
    }
}
