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
import bisq.common.util.MathUtils;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.ProofOfWorkService;
import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AuthorizationService {
    public final static int MIN_DIFFICULTY = 128;  // Math.pow(2, 7) = 128; 3 ms on old CPU, 1 ms on high-end CPU
    public final static int MAX_DIFFICULTY = 65536;  // Math.pow(2, 16) = 262144; 1000 ms on old CPU, 60 ms on high-end CPU
    public final static int DIFFICULTY_TOLERANCE = 50_000;

    private final ProofOfWorkService proofOfWorkService;
    // Keep track of message counter per connection to avoid reuse of pow
    private final Map<String, Set<Integer>> receivedMessageCountersByConnectionId = new ConcurrentHashMap<>();

    public AuthorizationService(ProofOfWorkService proofOfWorkService) {
        this.proofOfWorkService = proofOfWorkService;
    }

    public AuthorizationToken createToken(EnvelopePayloadMessage message,
                                          NetworkLoad networkLoad,
                                          String peerAddress,
                                          int messageCounter) {
        long ts = System.currentTimeMillis();
        double difficulty = calculateDifficulty(message, networkLoad);
        byte[] challenge = getChallenge(peerAddress, messageCounter);
        byte[] payload = getPayload(message);
        AuthorizationToken token = proofOfWorkService.mint(payload, challenge, difficulty)
                .thenApply(proofOfWork -> new AuthorizationToken(proofOfWork, messageCounter))
                .join();
        log.debug("Create token for {} took {} ms\n token={}, peersLoad={}, peerAddress={}",
                message.getClass().getSimpleName(), System.currentTimeMillis() - ts, token, networkLoad, peerAddress);
        return token;
    }

    public boolean isAuthorized(EnvelopePayloadMessage message,
                                AuthorizationToken authorizationToken,
                                NetworkLoad currentNetworkLoad,
                                String connectionId,
                                String myAddress) {
        return isAuthorized(message, authorizationToken, currentNetworkLoad, null, connectionId, myAddress);
    }

    public boolean isAuthorized(EnvelopePayloadMessage message,
                                AuthorizationToken authorizationToken,
                                NetworkLoad currentNetworkLoad,
                                @Nullable NetworkLoad previousNetworkLoad,
                                String connectionId,
                                String myAddress) {
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

        // Verify payload
        byte[] payload = getPayload(message);
        if (!Arrays.equals(payload, proofOfWork.getPayload())) {
            log.warn("Message payload not matching proof of work payload. getPayload(message).length={}; " +
                            "proofOfWork.getPayload().length={}",
                    payload.length, proofOfWork.getPayload().length);
            return false;
        }

        // Verify challenge
        if (!Arrays.equals(getChallenge(myAddress, messageCounter), proofOfWork.getChallenge())) {
            log.warn("Invalid challenge");
            return false;
        }

        // Verify difficulty
        double difficulty = calculateDifficulty(message, currentNetworkLoad);
        if (isInvalidDifficulty(difficulty, proofOfWork)) {
            if (previousNetworkLoad == null) {
                log.warn("Invalid difficulty using currentNetworkLoad. No previousNetworkLoad is provided.");
                return false;
            } else {
                log.info("Invalid difficulty using currentNetworkLoad. " +
                        "This can happen if the peer did not had the most recent networkLoad. We try again with the previous state.");
                difficulty = calculateDifficulty(message, previousNetworkLoad);
                if (isInvalidDifficulty(difficulty, proofOfWork)) {
                    log.warn("Invalid difficulty using previousNetworkLoad");
                    return false;
                }
            }
        }
        return proofOfWorkService.verify(proofOfWork);
    }

    private static boolean isInvalidDifficulty(double difficulty, ProofOfWork proofOfWork) {
        double difference = Math.abs(difficulty - proofOfWork.getDifficulty());
        if (difference > 0) {
            log.info("Calculated difficulty does not match difficulty from the proofOfWork object. \n" +
                            "difficulty={}; proofOfWork.getDifficulty()={}; difference={}",
                    difficulty, proofOfWork.getDifficulty(), difference);
        }
        boolean isInvalid = difference > DIFFICULTY_TOLERANCE;
        if (isInvalid) {
            log.warn("Calculated difficulty outside of tolerated difficulty difference from the proofOfWork object. \n" +
                            "difficulty={}; proofOfWork.getDifficulty()={}; difference={}",
                    difficulty, proofOfWork.getDifficulty(), difference);
        }
        return isInvalid;
    }

    private byte[] getPayload(EnvelopePayloadMessage message) {
        return message.toProto().toByteArray();
    }

    private byte[] getChallenge(String peerAddress, int messageCounter) {
        return DigestUtil.sha256(ByteArrayUtils.concat(peerAddress.getBytes(Charsets.UTF_8),
                BigInteger.valueOf(messageCounter).toByteArray()));
    }

    private double calculateDifficulty(EnvelopePayloadMessage message, NetworkLoad networkLoad) {
        double messageCostFactor = MathUtils.bounded(0.01, 1, message.getCostFactor());
        double loadValue = MathUtils.bounded(0.01, 1, networkLoad.getValue());
        double difficulty = MAX_DIFFICULTY * messageCostFactor + MAX_DIFFICULTY * loadValue;
        log.debug("calculated difficulty={}, Math.pow(2, {}), messageCostFactor={}, loadValue={}",
                difficulty, MathUtils.roundDouble(Math.log(difficulty) / MathUtils.LOG2, 2),
                messageCostFactor, loadValue);
        return MathUtils.bounded(MIN_DIFFICULTY, MAX_DIFFICULTY, difficulty);
    }
}
