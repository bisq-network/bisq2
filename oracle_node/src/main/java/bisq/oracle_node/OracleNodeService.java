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

package bisq.oracle_node;

import bisq.bonded_roles.node.bisq1_bridge.data.AuthorizedOracleNode;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.oracle_node.bisq1_bridge.Bisq1BridgeService;
import bisq.oracle_node.timestamp.TimestampService;
import bisq.persistence.PersistenceService;
import bisq.security.KeyGeneration;
import bisq.security.KeyPairService;
import bisq.security.SecurityService;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.UserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class OracleNodeService implements Service {


    @Getter
    public static class Config {
        private final String privateKey;
        private final String publicKey;
        private final String nickName;
        private final String userName;
        private final String signatureBase64;
        private final com.typesafe.config.Config bisq1Bridge;

        public Config(String privateKey,
                      String publicKey,
                      String nickName,
                      String userName,
                      String signatureBase64,
                      com.typesafe.config.Config bisq1Bridge) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.nickName = nickName;
            this.userName = userName;
            this.signatureBase64 = signatureBase64;
            this.bisq1Bridge = bisq1Bridge;
        }

        public static OracleNodeService.Config from(com.typesafe.config.Config config) {
            return new OracleNodeService.Config(config.getString("privateKey"),
                    config.getString("publicKey"),
                    config.getString("nickName"),
                    config.getString("userName"),
                    config.getString("signatureBase64"),
                    config.getConfig("bisq1Bridge"));
        }
    }

    private final IdentityService identityService;
    private final NetworkService networkService;
    private final KeyPairService keyPairService;
    private final ProofOfWorkService proofOfWorkService;

    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    @Getter
    private final Bisq1BridgeService bisq1BridgeService;
    @Getter
    private final TimestampService timestampService;
    private final String nickName;
    private final String userName;
    private final String signatureBase64;
    @Nullable
    private Scheduler scheduler;

    public OracleNodeService(OracleNodeService.Config config,
                             IdentityService identityService,
                             NetworkService networkService,
                             PersistenceService persistenceService,
                             SecurityService securityService,
                             UserService userService) {
        this.identityService = identityService;
        this.networkService = networkService;
        keyPairService = securityService.getKeyPairService();
        proofOfWorkService = securityService.getProofOfWorkService();

        nickName = config.getNickName();
        userName = config.getUserName();
        signatureBase64 = config.getSignatureBase64();
        checkArgument(StringUtils.isNotEmpty(nickName));
        checkArgument(StringUtils.isNotEmpty(userName));
        checkArgument(StringUtils.isNotEmpty(signatureBase64));

        String privateKey = config.getPrivateKey();
        String publicKey = config.getPublicKey();
        checkArgument(StringUtils.isNotEmpty(privateKey));
        checkArgument(StringUtils.isNotEmpty(publicKey));

        authorizedPrivateKey = getAuthorizedPrivateKey(privateKey);
        authorizedPublicKey = getAuthorizedPublicKey(publicKey);

        Bisq1BridgeService.Config bisq1BridgeConfig = Bisq1BridgeService.Config.from(config.getBisq1Bridge());
        bisq1BridgeService = new Bisq1BridgeService(bisq1BridgeConfig,
                networkService,
                persistenceService,
                identityService,
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
        return createMyAuthorizedOracleNode()
                .thenCompose(authorizedOracleNode -> {
                    bisq1BridgeService.setAuthorizedOracleNode(authorizedOracleNode);
                    return bisq1BridgeService.initialize()
                            .thenCompose(result -> {
                                timestampService.setAuthorizedOracleNode(authorizedOracleNode);
                                return timestampService.initialize();
                            });
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (scheduler != null) {
            scheduler.stop();
        }
        return bisq1BridgeService.shutdown()
                .thenCompose(result -> timestampService.shutdown());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<AuthorizedOracleNode> createMyAuthorizedOracleNode() {
        return identityService.createAndInitializeDefaultIdentity()
                .thenApply(identity -> {
                    AuthorizedOracleNode authorizedOracleNode = new AuthorizedOracleNode(identity.getNetworkId(), userName, signatureBase64);
                    scheduler = Scheduler.run(() -> publishAuthorizedOracleNode(authorizedOracleNode, identity.getNodeIdAndKeyPair())).periodically(0, 15, TimeUnit.DAYS);
                    return authorizedOracleNode;
                });
    }

    private CompletableFuture<Boolean> publishAuthorizedOracleNode(AuthorizedOracleNode authorizedOracleNode, NetworkIdWithKeyPair nodeIdAndKeyPair) {
        return networkService.publishAuthorizedData(authorizedOracleNode,
                        nodeIdAndKeyPair,
                        authorizedPrivateKey,
                        authorizedPublicKey)
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