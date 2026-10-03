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

import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.MessageCostPolicy;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.token.equi_hash.EquiHashTokenService;
import bisq.network.p2p.node.authorization.token.hash_cash_v2.HashCashV2TokenService;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AuthorizationService {
    @Getter
    public static final class Config {
        private final List<AuthorizationTokenType> myPreferredAuthorizationTokenTypes; // Lower list index means higher preference

        public static Config from(com.typesafe.config.Config config) {
            return new Config(new ArrayList<>(config.getEnumList(AuthorizationTokenType.class, "myPreferredAuthorizationTokenTypes")));
        }

        public Config(List<AuthorizationTokenType> myPreferredAuthorizationTokenTypes) {
            this.myPreferredAuthorizationTokenTypes = myPreferredAuthorizationTokenTypes;
        }
    }

    private final List<AuthorizationTokenType> myPreferredAuthorizationTokenTypes; // Lower list index means higher preference
    @Getter
    private final Map<AuthorizationTokenType, AuthorizationTokenService<? extends AuthorizationToken>> supportedServices = new HashMap<>();
    private final HashCashProofOfWorkService hashCashProofOfWorkService;

    public AuthorizationService(Config config,
                                HashCashProofOfWorkService hashCashProofOfWorkService,
                                EquihashProofOfWorkService equihashProofOfWorkService,
                                Set<Feature> features) {
        myPreferredAuthorizationTokenTypes = config.getMyPreferredAuthorizationTokenTypes();
        this.hashCashProofOfWorkService = hashCashProofOfWorkService;

        features.stream()
                .flatMap(feature -> AuthorizationTokenType.fromFeature(feature).stream())
                .forEach(supportedFilterType -> {
                    switch (supportedFilterType) {
                        case EQUI_HASH:
                            supportedServices.put(supportedFilterType, new EquiHashTokenService(equihashProofOfWorkService));
                            break;
                        case HASH_CASH_V2:
                            supportedServices.put(supportedFilterType, new HashCashV2TokenService(hashCashProofOfWorkService));
                            break;
                        default:
                            throw new IllegalArgumentException("Undefined filterType " + supportedFilterType);
                    }
                });
    }

    public AuthorizationToken createToken(EnvelopePayloadMessage message,
                                          NetworkLoad networkLoad,
                                          String peerAddress,
                                          int messageCounter,
                                          Collection<Feature> peersFeatures) {
        AuthorizationTokenType preferredAuthorizationTokenType = selectAuthorizationTokenType(peersFeatures);
        checkArgument(supportedServices.containsKey(preferredAuthorizationTokenType),
                "No locally supported authorization service for %s", preferredAuthorizationTokenType);
        return supportedServices.get(preferredAuthorizationTokenType).createToken(message,
                networkLoad,
                peerAddress,
                messageCounter);
    }

    public boolean isAuthorized(EnvelopePayloadMessage message,
                                AuthorizationToken authorizationToken,
                                NetworkLoad currentNetworkLoad,
                                Set<Integer> receivedMessageCounters,
                                String myAddress) {
        return isAuthorized(message,
                authorizationToken,
                currentNetworkLoad,
                Optional.empty(),
                receivedMessageCounters,
                myAddress);
    }

    public boolean isAuthorized(EnvelopePayloadMessage message,
                                AuthorizationToken authorizationToken,
                                NetworkLoad currentNetworkLoad,
                                Optional<NetworkLoad> previousNetworkLoad,
                                Set<Integer> receivedMessageCounters,
                                String myAddress) {
        AuthorizationTokenType authorizationTokenType = authorizationToken.getAuthorizationTokenType();
        if (!supportedServices.containsKey(authorizationTokenType)) {
            log.warn("Not supported authorizationTokenType {}", authorizationTokenType);
            return false;
        }
        return supportedServices.get(authorizationTokenType).isAuthorized(message,
                authorizationToken,
                currentNetworkLoad,
                previousNetworkLoad,
                receivedMessageCounters,
                myAddress);
    }

    /**
     * Authorizes the handshake request of a node of a version up to 2.1.13, which carries the legacy HASH_CASH token
     * (see docs/specifications/network/authorization-tokens.md). The parameter type limits the legacy method to that
     * message: no other message can be authorized with it.
     * <p>
     * The checks are those of the earlier versions, with two differences. The payload must be the request serialized
     * for the hash: the earlier versions also accepted the complete serialization. The difficulty has no tolerance:
     * the requester does not know our network load yet, so both sides use the fixed initial load. The proof of work
     * then needs at least the work which those versions do for a handshake request.
     * <p>
     * As for the other methods, the challenge binds a proof of work to this node, but not to one connection. The
     * date check of the handshake limits how long a request can be used again.
     */
    public boolean isLegacyHandshakeRequestAuthorized(ConnectionHandshake.Request request,
                                                      LegacyHandshakeToken token,
                                                      String myAddress) {
        ProofOfWork proofOfWork = token.getProofOfWork();
        // The log contains no received values
        if (!Arrays.equals(request.serializeForHash(), proofOfWork.getPayload())) {
            log.warn("The payload of the legacy handshake token does not match the handshake request");
            return false;
        }
        byte[] challenge = HashCashV2TokenService.getChallenge(myAddress, token.getMessageCounter());
        if (!Arrays.equals(challenge, proofOfWork.getChallenge())) {
            log.warn("The challenge of the legacy handshake token is invalid");
            return false;
        }
        double expectedDifficulty = HashCashV2TokenService.calculateDifficulty(MessageCostPolicy.getCostFactor(request),
                NetworkLoad.INITIAL_NETWORK_LOAD);
        // Not `difficulty < expectedDifficulty`, which is false for NaN
        if (!(proofOfWork.getDifficulty() >= expectedDifficulty)) {
            log.warn("The difficulty of the legacy handshake token is too low");
            return false;
        }
        return hashCashProofOfWorkService.verify(proofOfWork);
    }

    // Get first match with peers feature based on order of myPreferredFilterTypes
    private AuthorizationTokenType selectAuthorizationTokenType(Collection<Feature> peersFeatures) {
        return selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
    }

    @VisibleForTesting
    static AuthorizationTokenType selectAuthorizationTokenType(List<AuthorizationTokenType> myPreferredAuthorizationTokenTypes,
                                                               Collection<Feature> peersFeatures) {
        checkArgument(!myPreferredAuthorizationTokenTypes.isEmpty(), "myPreferredAuthorizationTokenTypes must not be empty");
        if (peersFeatures.isEmpty()) {
            return myPreferredAuthorizationTokenTypes.get(0);
        }
        List<AuthorizationTokenType> peersAuthorizationTokenTypes = toAuthorizationTypes(peersFeatures);
        checkArgument(!peersAuthorizationTokenTypes.isEmpty(), "Peer has no supported authorization token type");
        return myPreferredAuthorizationTokenTypes.stream()
                .filter(peersAuthorizationTokenTypes::contains)
                .findFirst()
                .orElseGet(() -> peersAuthorizationTokenTypes.get(0));
    }

    private static List<AuthorizationTokenType> toAuthorizationTypes(Collection<Feature> features) {
        return features.stream()
                .flatMap(feature -> AuthorizationTokenType.fromFeature(feature).stream())
                .collect(Collectors.toList());
    }
}
