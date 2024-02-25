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
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.token.equi_hash.EquiHashTokenService;
import bisq.network.p2p.node.authorization.token.hash_cash.HashCashTokenService;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private final Map<AuthorizationTokenType, AuthorizationTokenService<? extends AuthorizationToken>> supportedServices = new HashMap<>();

    // Keep track of message counter per connection to avoid reuse of pow
    private final Map<String, Set<Integer>> receivedMessageCountersByConnectionId = new ConcurrentHashMap<>();

    public AuthorizationService(Config config,
                                HashCashService hashCashService,
                                EquihashProofOfWorkService equihashProofOfWorkService,
                                Set<Feature> features) {
        myPreferredAuthorizationTokenTypes = config.getMyPreferredAuthorizationTokenTypes();

        features.stream()
                .flatMap(feature -> AuthorizationTokenType.fromFeature(feature).stream())
                .forEach(supportedFilterType -> {
                    switch (supportedFilterType) {
                        case HASH_CASH:
                            supportedServices.put(supportedFilterType, new HashCashTokenService(hashCashService));
                            break;
                        case EQUI_HASH:
                            supportedServices.put(supportedFilterType, new EquiHashTokenService(equihashProofOfWorkService));
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
                                          List<Feature> features) {
        AuthorizationTokenType preferredAuthorizationTokenType = getPreferredAuthorizationType(features);
        return supportedServices.get(preferredAuthorizationTokenType).createToken(message,
                networkLoad,
                peerAddress,
                messageCounter);
    }

    public boolean isAuthorized(EnvelopePayloadMessage message,
                                AuthorizationToken authorizationToken,
                                NetworkLoad currentNetworkLoad,
                                String connectionId,
                                String myAddress) {
        return isAuthorized(message,
                authorizationToken,
                currentNetworkLoad,
                Optional.empty(),
                connectionId,
                myAddress);
    }

    public boolean isAuthorized(EnvelopePayloadMessage message,
                                AuthorizationToken authorizationToken,
                                NetworkLoad currentNetworkLoad,
                                Optional<NetworkLoad> previousNetworkLoad,
                                String connectionId,
                                String myAddress) {
        return supportedServices.get(authorizationToken.getAuthorizationTokenType()).isAuthorized(message,
                authorizationToken,
                currentNetworkLoad,
                previousNetworkLoad,
                connectionId,
                myAddress);
    }

    // Get first match with peers feature based on order of myPreferredFilterTypes
    private AuthorizationTokenType getPreferredAuthorizationType(List<Feature> peersFeatures) {
        List<AuthorizationTokenType> peersAuthorizationTokenTypes = toAuthorizationTypes(peersFeatures);
        return myPreferredAuthorizationTokenTypes.stream()
                .filter(peersAuthorizationTokenTypes::contains)
                .findFirst()
                .orElse(AuthorizationTokenType.HASH_CASH);
    }

    private List<AuthorizationTokenType> toAuthorizationTypes(List<Feature> features) {
        return features.stream()
                .flatMap(feature -> AuthorizationTokenType.fromFeature(feature).stream())
                .collect(Collectors.toList());
    }
}
