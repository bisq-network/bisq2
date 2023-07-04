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

package bisq.oracle.node;

import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle.node.bisq1_bridge.Bisq1BridgeService;
import bisq.oracle.node.timestamp.TimestampService;
import bisq.persistence.PersistenceService;
import bisq.security.KeyGeneration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class OracleNodeService implements Service {
    @Getter
    public static class Config {
        private final String privateKey;
        private final String publicKey;
        private final com.typesafe.config.Config bisq1Bridge;

        public Config(String privateKey,
                      String publicKey,
                      com.typesafe.config.Config bisq1Bridge) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.bisq1Bridge = bisq1Bridge;
        }

        public static OracleNodeService.Config from(com.typesafe.config.Config config) {
            return new OracleNodeService.Config(config.getString("privateKey"),
                    config.getString("publicKey"),
                    config.getConfig("bisq1Bridge"));
        }
    }

    private final IdentityService identityService;
    private final NetworkService networkService;
    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    @Getter
    private final Bisq1BridgeService bisq1BridgeService;
    @Getter
    private final TimestampService timestampService;

    public OracleNodeService(OracleNodeService.Config config,
                             IdentityService identityService,
                             NetworkService networkService,
                             PersistenceService persistenceService) {
        this.identityService = identityService;
        this.networkService = networkService;

        String privateKey = config.getPrivateKey();
        String publicKey = config.getPublicKey();
        checkArgument(StringUtils.isNotEmpty(privateKey));
        checkArgument(StringUtils.isNotEmpty(publicKey));
        authorizedPrivateKey = getAuthorizedPrivateKey(privateKey);
        authorizedPublicKey = getAuthorizedPublicKey(publicKey);

        Bisq1BridgeService.Config bisq1BridgeConfig = Bisq1BridgeService.Config.from(config.getBisq1Bridge());
        bisq1BridgeService = new Bisq1BridgeService(bisq1BridgeConfig,
                networkService,
                identityService,
                persistenceService,
                authorizedPrivateKey,
                authorizedPublicKey);

        timestampService = new TimestampService(persistenceService,
                identityService,
                networkService,
                authorizedPrivateKey,
                authorizedPublicKey);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        identityService.getOrCreateIdentity(IdentityService.DEFAULT)
                .whenComplete((identity, throwable) -> {
                    AuthorizedOracleNode data = new AuthorizedOracleNode(identity.getNetworkId());
                    publishAuthorizedData(data);
                });

        return bisq1BridgeService.initialize()
                .thenCompose(result -> timestampService.initialize());
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return bisq1BridgeService.shutdown()
                .thenCompose(result -> timestampService.shutdown());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Boolean> publishAuthorizedData(AuthorizedDistributedData data) {
        return identityService.getOrCreateIdentity(IdentityService.DEFAULT)
                .thenCompose(identity -> networkService.publishAuthorizedData(data,
                        identity.getNodeIdAndKeyPair(),
                        authorizedPrivateKey,
                        authorizedPublicKey))
                .thenApply(broadCastDataResult -> true);
    }

    private static PublicKey getAuthorizedPublicKey(String publicKey) {
        try {
            return KeyGeneration.generatePublic(Hex.decode(publicKey));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static PrivateKey getAuthorizedPrivateKey(String privateKey) {
        try {
            return KeyGeneration.generatePrivate(Hex.decode(privateKey));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}