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

package bisq.network.p2p.node.authorization;

import bisq.common.util.ByteArrayUtils;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.Load;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.ProofOfWorkService;
import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AuthorizationService {
    private final ProofOfWorkService proofOfWorkService;
    // Keep track of message counter per connection to avoid reuse of pow
    private final Map<String, Set<Integer>> receivedMessageCountersByConnectionId = new ConcurrentHashMap<>();

    public AuthorizationService(ProofOfWorkService proofOfWorkService) {
        this.proofOfWorkService = proofOfWorkService;
    }

    public AuthorizationToken createToken(NetworkMessage message, Load peersLoad, String peerAddress, int messageCounter) {
        long ts = System.currentTimeMillis();
        AuthorizationToken token = proofOfWorkService.mint(getPayload(message), getChallenge(peerAddress, messageCounter), calculateDifficulty(message, peersLoad))
                .thenApply(proofOfWork -> new AuthorizationToken(proofOfWork, messageCounter))
                .join();
        log.debug("Create token for {} took {} ms\n token={}, peersLoad={}, peerAddress={}",
                message.getClass().getSimpleName(), System.currentTimeMillis() - ts, token, peersLoad, peerAddress);
        return token;
    }

    public boolean isAuthorized(NetworkMessage message, AuthorizationToken authorizationToken, Load myLoad, String connectionId, String myAddress) {
        ProofOfWork proofOfWork = authorizationToken.getProofOfWork();
        int messageCounter = authorizationToken.getMessageCounter();

        // Verify that pow is not reused
        Set<Integer> receivedMessageCounters;
        if (receivedMessageCountersByConnectionId.containsKey(connectionId)) {
            receivedMessageCounters = receivedMessageCountersByConnectionId.get(connectionId);
            if (receivedMessageCounters.contains(messageCounter)) {
                log.warn("Invalid receivedMessageCounters. We received the proofOfWork for that message already.");
                return false;
            }
        } else {
            receivedMessageCounters = new HashSet<>();
            receivedMessageCountersByConnectionId.put(connectionId, receivedMessageCounters);
        }
        receivedMessageCounters.add(messageCounter);

        // Verify difficulty
        if (calculateDifficulty(message, myLoad) != proofOfWork.getDifficulty()) {
            log.warn("Invalid difficulty");
            return false;
        }

        // Verify payload
        if (!Arrays.equals(getPayload(message), proofOfWork.getPayload())) {
            log.warn("Invalid payload");
            return false;
        }

        // Verify challenge
        if (!Arrays.equals(getChallenge(myAddress, messageCounter), proofOfWork.getChallenge())) {
            log.warn("Invalid challenge");
            return false;
        }

        log.debug("Verify token for {}. token={}, myLoad={}, myAddress={}",
                message.getClass().getSimpleName(), authorizationToken, myLoad, myAddress);
        return proofOfWorkService.verify(proofOfWork);
    }

    private byte[] getPayload(NetworkMessage message) {
        return message.toProto().toByteArray();
    }

    private byte[] getChallenge(String peerAddress, int messageCounter) {
        return ByteArrayUtils.concat(peerAddress.getBytes(Charsets.UTF_8),
                BigInteger.valueOf(messageCounter).toByteArray());
    }

    private double calculateDifficulty(NetworkMessage message, Load load) {
        //todo impl formula and add costs to messages
        int cost = message.getCost();
        int loadFactor = load.getFactor();
        return cost * loadFactor;
        // return 1048576; // = Math.pow(2, 20) = 1048576; -> high value which takes several seconds
    }
}
