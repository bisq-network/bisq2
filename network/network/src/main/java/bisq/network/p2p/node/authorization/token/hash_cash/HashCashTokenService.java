package bisq.network.p2p.node.authorization.token.hash_cash;

import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.util.ByteArrayUtils;
import bisq.common.util.MathUtils;
import bisq.common.util.StringUtils;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.AuthorizationTokenService;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import com.google.common.base.Charsets;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class HashCashTokenService extends AuthorizationTokenService<HashCashToken> {
    public final static double MIN_MESSAGE_COST = 0.01;
    public final static double MIN_LOAD = 0.01;
    public final static int MIN_DIFFICULTY = 128;  // 2^7 = 128; 3 ms on old CPU, 1 ms on high-end CPU. Would result in an average time of 1-5 ms on high-end CPU
    public final static int TARGET_DIFFICULTY = 65536;  // 2^16 = 262144; 1000 ms on old CPU, 60-140 ms on high-end CPU. Would result in an average time of 100-150 ms on high-end CPU
    public final static int MAX_DIFFICULTY = 1048576;  // 2^20 = 1048576; Would result in an average time 0.5-2 sec on high-end CPU
    public final static int DIFFICULTY_TOLERANCE = 50_000;

    private final HashCashProofOfWorkService proofOfWorkService;
    // Keep track of message counter per connection to avoid reuse of pow
    private final Map<String, Set<Integer>> receivedMessageCountersByConnectionId = new ConcurrentHashMap<>();
    @Getter
    private final Metrics metrics = new Metrics();

    public HashCashTokenService(HashCashProofOfWorkService proofOfWorkService) {
        this.proofOfWorkService = proofOfWorkService;
    }

    @Override
    public HashCashToken createToken(EnvelopePayloadMessage message,
                                     NetworkLoad networkLoad,
                                     String peerAddress,
                                     int messageCounter) {
        long ts = System.currentTimeMillis();
        double difficulty = calculateDifficulty(message, networkLoad);
        byte[] challenge = getChallenge(peerAddress, messageCounter);
        byte[] payload = getPayload(message);
        ProofOfWork proofOfWork = proofOfWorkService.mint(payload, challenge, difficulty);
        long duration = System.currentTimeMillis() - ts;
        metrics.update(duration, networkLoad.getLoad());

        HashCashToken token = new HashCashToken(proofOfWork, messageCounter);
        log.debug("Create HashCashToken for {} took {} ms" +
                        "\ncostFactor={}" +
                        "\ngetPayload(message)={}" +
                        "\nnetworkLoad={}" +
                        "\nhashCashToken={}",
                message.getClass().getSimpleName(), duration,
                message.getCostFactor(),
                Hex.encode(payload),
                networkLoad,
                token);
        return token;
    }

    @Override
    public boolean isAuthorized(EnvelopePayloadMessage message,
                                AuthorizationToken authorizationToken,
                                NetworkLoad currentNetworkLoad,
                                Optional<NetworkLoad> previousNetworkLoad,
                                String connectionId,
                                String myAddress) {

        HashCashToken hashCashToken = (HashCashToken) authorizationToken;
        ProofOfWork proofOfWork = hashCashToken.getProofOfWork();
        int messageCounter = hashCashToken.getMessageCounter();

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
        byte[] proofOfWorkPayload = proofOfWork.getPayload();
        if (!Arrays.equals(payload, proofOfWorkPayload)) {
            // We try again with ignoring ExcludeForHash annotations by using the serialize() method.
            byte[] payloadWithoutUsingExcludeForHash = message.serialize();
            if (Arrays.equals(payloadWithoutUsingExcludeForHash, proofOfWorkPayload)) {
                log.info("Proof of work payload not matching message.serializeForHash() but " +
                        "matching message.serialize(). This is expected for certain messages from " +
                        "nodes which do not run the latest version.");
            } else {
                log.warn("Message payload not matching proof of work payload. " +
                                "getPayload(message)={};\n" +
                                "proofOfWork.getPayload()={};\n" +
                                "getPayload(message).length={};\n" +
                                "proofOfWork.getPayload().length={}\n" +
                                "message={}",
                        StringUtils.truncate(Hex.encode(payload), 200),
                        StringUtils.truncate(Hex.encode(proofOfWorkPayload), 200),
                        payload.length,
                        proofOfWorkPayload.length,
                        StringUtils.truncate(message.toString(), 5000));
                return false;
            }
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
    // It is likely caused when there is a flood of messages as it is the case when the oracle node republishes its data.
    // During that time the local network load rises, but we might not have exchanges with our peers our network load 
    // (3-5 min interval) and thus peers use a too low network load to calculate the difficulty for messages sent to us.
    // We could trigger a network load exchange if detect a certain level of deviation but as long we don't observe 
    // those deviations in normal network mode, we ignore it.
    private boolean isDifficultyInvalid(EnvelopePayloadMessage message,
                                        double proofOfWorkDifficulty,
                                        NetworkLoad currentNetworkLoad,
                                        Optional<NetworkLoad> previousNetworkLoad) {
        log.debug("isDifficultyInvalid/currentNetworkLoad: message.getCostFactor()={}, networkLoad.getValue()={}",
                message.getCostFactor(), currentNetworkLoad.getLoad());
        double expectedDifficulty = calculateDifficulty(message, currentNetworkLoad);
        if (proofOfWorkDifficulty >= expectedDifficulty) {
            // We don't want to call calculateDifficulty with the previousNetworkLoad if we are not in dev mode.
            if (DevMode.isDevMode() && proofOfWorkDifficulty > expectedDifficulty && previousNetworkLoad.isPresent()) {
                // Might be that the difficulty was using the previous network load
                double expectedPreviousDifficulty = calculateDifficulty(message, previousNetworkLoad.get());
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
        if (previousNetworkLoad.isEmpty()) {
            log.debug("No previous network load available");
            if (missing <= DIFFICULTY_TOLERANCE) {
                log.info("Difficulty of current network load deviates from the proofOfWork difficulty but is inside the tolerated range.\n" +
                                "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}",
                        deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty);
                return false;
            }

            log.warn("Difficulty of current network load deviates from the proofOfWork difficulty and is outside the tolerated range.\n" +
                            "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}",
                    deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty);
            return true;
        }

        log.debug("isDifficultyInvalid/previousNetworkLoad: message.getCostFactor()={}, networkLoad.getValue()={}",
                message.getCostFactor(), previousNetworkLoad.get().getLoad());
        double expectedPreviousDifficulty = calculateDifficulty(message, previousNetworkLoad.get());
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
                            "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}",
                    deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty);
            return false;
        }

        double missingUsingPrevious = expectedPreviousDifficulty - proofOfWorkDifficulty;
        if (missingUsingPrevious <= DIFFICULTY_TOLERANCE) {
            deviationToTolerance = MathUtils.roundDouble(missingUsingPrevious / DIFFICULTY_TOLERANCE * 100, 2);
            deviationToExpectedDifficulty = MathUtils.roundDouble(missingUsingPrevious / expectedPreviousDifficulty * 100, 2);
            log.info("Difficulty of previous network load deviates from the proofOfWork difficulty but is inside the tolerated range.\n" +
                            "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}",
                    deviationToTolerance, deviationToExpectedDifficulty, expectedPreviousDifficulty, proofOfWorkDifficulty);
            return false;
        }

        log.warn("Difficulties of current and previous network load deviate from the proofOfWork difficulty and are outside the tolerated range.\n" +
                        "deviationToTolerance={}%; deviationToExpectedDifficulty={}%; expectedDifficulty={}; proofOfWorkDifficulty={}",
                deviationToTolerance, deviationToExpectedDifficulty, expectedDifficulty, proofOfWorkDifficulty);
        return true;
    }

    private byte[] getPayload(EnvelopePayloadMessage message) {
        return message.serializeForHash();
    }

    private byte[] getChallenge(String peerAddress, int messageCounter) {
        return DigestUtil.sha256(ByteArrayUtils.concat(peerAddress.getBytes(Charsets.UTF_8),
                BigInteger.valueOf(messageCounter).toByteArray()));
    }

    private double calculateDifficulty(EnvelopePayloadMessage message, NetworkLoad networkLoad) {
        double messageCostFactor = MathUtils.bounded(MIN_MESSAGE_COST, 1, message.getCostFactor());
        double load = MathUtils.bounded(MIN_LOAD, 1, networkLoad.getLoad());
        double difficulty = TARGET_DIFFICULTY * messageCostFactor * load * networkLoad.getDifficultyAdjustmentFactor();
        return MathUtils.bounded(MIN_DIFFICULTY, MAX_DIFFICULTY, difficulty);
    }

    @Slf4j

    public static class Metrics {
        private final List<Long> aggregatedPoWDuration = new CopyOnWriteArrayList<>();
        private final List<Double> aggregatedNetworkLoadValues = new CopyOnWriteArrayList<>();
        @Getter
        private long accumulatedPoWDuration;
        @Getter
        private int numPowTokensCreated;
        @Getter
        private long averagePowTimePerMessage;
        @Getter
        private double averageNetworkLoad;

        void update(long duration, double networkLoad) {
            accumulatedPoWDuration += duration;
            aggregatedPoWDuration.add(duration);
            aggregatedNetworkLoadValues.add(networkLoad);
            numPowTokensCreated = aggregatedPoWDuration.size();
            if (numPowTokensCreated % 10 == 0) {
                averagePowTimePerMessage = MathUtils.roundDoubleToLong(aggregatedPoWDuration.stream().mapToLong(e -> e).average().orElse(0D));
                averageNetworkLoad = MathUtils.roundDouble(aggregatedNetworkLoadValues.stream().mapToDouble(e -> e).average().orElse(0D), 4);

                if (numPowTokensCreated % 100 == 0) {
                    if (averagePowTimePerMessage > 1000) {
                        log.warn("Average time/message used for PoW is very high");
                    } else if (averagePowTimePerMessage > 300) {
                        log.warn("Average time/message used for PoW is higher as expected");
                    }
                    log.info("Total time used for PoW: {} sec; Average time/message used for PoW: {} ms; Average network load value: {}; Number of messages: {}",
                            MathUtils.roundDoubleToLong(accumulatedPoWDuration / 1000d),
                            averagePowTimePerMessage,
                            averageNetworkLoad,
                            numPowTokensCreated);
                    if (aggregatedPoWDuration.size() > 100_000) {
                        log.warn("aggregatedPoWDuration is getting too large. We clear the list.");
                        aggregatedPoWDuration.clear();
                        aggregatedNetworkLoadValues.clear();
                    }
                }
            }
        }
    }
}
