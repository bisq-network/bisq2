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

import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
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
            log.warn("Message payload not matching proof of work payload. " +
                            "getPayload(message)={}; proofOfWork.getPayload()={}; " +
                            "getPayload(message).length={}; proofOfWork.getPayload().length={}",
                    Hex.encode(payload), Hex.encode(proofOfWork.getPayload()),
                    payload.length, proofOfWork.getPayload().length);
            return false;
        }

        // Verify challenge
        if (!Arrays.equals(getChallenge(myAddress, messageCounter), proofOfWork.getChallenge())) {
            log.warn("Invalid challenge");
            return false;
        }

        // Verify difficulty
        if (isDifficultyInvalid(message, proofOfWork.getDifficulty(), currentNetworkLoad, previousNetworkLoad)) {
            return false;
        }
        return proofOfWorkService.verify(proofOfWork);
    }

    // We check the difficulty used for the proof of work if it matches the current network load or if available the
    // previous network load. If the difference is inside a tolerance range we consider it still valid, but it should
    // be investigated why that happens, thus we log those cases.
    private boolean isDifficultyInvalid(EnvelopePayloadMessage message,
                                        double proofOfWorkDifficulty,
                                        NetworkLoad currentNetworkLoad,
                                        NetworkLoad previousNetworkLoad) {
        log.debug("isDifficultyInvalid/currentNetworkLoad: message.getCostFactor()={}, networkLoad.getValue()={}",
                message.getCostFactor(), currentNetworkLoad.getValue());
        double expectedDifficulty = calculateDifficulty(message, currentNetworkLoad);
        if (proofOfWorkDifficulty >= expectedDifficulty) {

            // We don't want to call calculateDifficulty with the previousNetworkLoad if we are not in dev mode.
            if (DevMode.isDevMode() && proofOfWorkDifficulty > expectedDifficulty) {
                // Might be that the difficulty was using the previous network load
                double expectedPreviousDifficulty = calculateDifficulty(message, previousNetworkLoad);
                if (proofOfWorkDifficulty != expectedPreviousDifficulty) {
                    log.warn("Unexpected high difficulty provided. This might be a bug (but valid as provided difficulty is larger as expected): " +
                                    "expectedDifficulty={}; expectedPreviousDifficulty={}; proofOfWorkDifficulty={}",
                            expectedDifficulty, expectedPreviousDifficulty, proofOfWorkDifficulty);
                }
            }
            return false;
        }

        double missing = expectedDifficulty - proofOfWorkDifficulty;
        double deviationToTolerance = MathUtils.roundDouble(missing / DIFFICULTY_TOLERANCE * 100, 2);
        double deviationToExpectedDifficulty = MathUtils.roundDouble(missing / expectedDifficulty * 100, 2);
        if (previousNetworkLoad == null) {
            log.debug("No previous network load available");
            if (missing <= DIFFICULTY_TOLERANCE) {
                log.info("Difficulty of current network load deviates from the proofOfWork difficulty but is inside the tolerated range.\n" +
                                "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}; DIFFICULTY_TOLERANCE={}",
                        deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty, DIFFICULTY_TOLERANCE);
                return false;
            }

            log.warn("Difficulty of current network load deviates from the proofOfWork difficulty and is outside the tolerated range.\n" +
                            "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}; DIFFICULTY_TOLERANCE={}",
                    deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty, DIFFICULTY_TOLERANCE);
            return true;
        }

        log.debug("isDifficultyInvalid/previousNetworkLoad: message.getCostFactor()={}, networkLoad.getValue()={}",
                message.getCostFactor(), previousNetworkLoad.getValue());
        double expectedPreviousDifficulty = calculateDifficulty(message, previousNetworkLoad);
        if (proofOfWorkDifficulty >= expectedPreviousDifficulty) {
            log.debug("Difficulty of previous network load is correct");
            if (proofOfWorkDifficulty > expectedPreviousDifficulty) {
                log.warn("Unexpected high difficulty provided. This might be a bug (but valid as provided difficulty is larger as expected): " +
                                "expectedPreviousDifficulty={}; proofOfWorkDifficulty={}",
                        expectedPreviousDifficulty, proofOfWorkDifficulty);
            }
            return false;
        }

        if (missing <= DIFFICULTY_TOLERANCE) {
            log.info("Difficulty of current network load deviates from the proofOfWork difficulty but is inside the tolerated range.\n" +
                            "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}; DIFFICULTY_TOLERANCE={}",
                    deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty, DIFFICULTY_TOLERANCE);
            return false;
        }

        double missingUsingPrevious = expectedPreviousDifficulty - proofOfWorkDifficulty;
        if (missingUsingPrevious <= DIFFICULTY_TOLERANCE) {
            deviationToTolerance = MathUtils.roundDouble(missingUsingPrevious / DIFFICULTY_TOLERANCE * 100, 2);
            deviationToExpectedDifficulty = MathUtils.roundDouble(missingUsingPrevious / expectedPreviousDifficulty * 100, 2);
            log.info("Difficulty of previous network load deviates from the proofOfWork difficulty but is inside the tolerated range.\n" +
                            "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}; DIFFICULTY_TOLERANCE={}",
                    deviationToTolerance, deviationToExpectedDifficulty, expectedPreviousDifficulty, proofOfWorkDifficulty, DIFFICULTY_TOLERANCE);
            return false;
        }

        log.warn("Difficulties of current and previous network load deviate from the proofOfWork difficulty and are outside the tolerated range.\n" +
                        "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}; DIFFICULTY_TOLERANCE={}",
                deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty, DIFFICULTY_TOLERANCE);
        return true;
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
