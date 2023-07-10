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

import bisq.bonded_roles.AuthorizedBondedRole;
import bisq.bonded_roles.AuthorizedBondedRolesService;
import bisq.bonded_roles.AuthorizedOracleNode;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.oracle_node.bisq1_bridge.Bisq1BridgeService;
import bisq.oracle_node.timestamp.TimestampService;
import bisq.persistence.PersistenceService;
import bisq.security.KeyGeneration;
import bisq.security.SecurityService;
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
        private final String bondUserName;
        private final String signatureBase64;
        private final String keyId;
        private final com.typesafe.config.Config bisq1Bridge;

        public Config(String privateKey,
                      String publicKey,
                      String bondUserName,
                      String signatureBase64,
                      String keyId,
                      com.typesafe.config.Config bisq1Bridge) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.bondUserName = bondUserName;
            this.signatureBase64 = signatureBase64;
            this.keyId = keyId;
            this.bisq1Bridge = bisq1Bridge;
        }

        public static OracleNodeService.Config from(com.typesafe.config.Config config) {
            return new OracleNodeService.Config(config.getString("privateKey"),
                    config.getString("publicKey"),
                    config.getString("bondUserName"),
                    config.getString("signatureBase64"),
                    config.getString("keyId"),
                    config.getConfig("bisq1Bridge"));
        }
    }

    private final IdentityService identityService;
    private final NetworkService networkService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;

    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    @Getter
    private final Bisq1BridgeService bisq1BridgeService;
    @Getter
    private final TimestampService timestampService;
    private final String bondUserName;
    private final String signatureBase64;
    private final String keyId;
    private AuthorizedOracleNode authorizedOracleNode;
    private Identity identity;

    @Nullable
    private Scheduler startupScheduler, scheduler;

    public OracleNodeService(Config config,
                             IdentityService identityService,
                             NetworkService networkService,
                             PersistenceService persistenceService,
                             SecurityService securityService,
                             AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.identityService = identityService;
        this.networkService = networkService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;

        bondUserName = config.getBondUserName();
        signatureBase64 = config.getSignatureBase64();
        keyId = config.getKeyId();
        checkArgument(StringUtils.isNotEmpty(bondUserName));
        checkArgument(StringUtils.isNotEmpty(signatureBase64));
        checkArgument(StringUtils.isNotEmpty(keyId));

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
                authorizedPrivateKey,
                authorizedPublicKey,
                keyId);

        timestampService = new TimestampService(persistenceService,
                networkService,
                authorizedPrivateKey,
                authorizedPublicKey);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        return identityService.createAndInitializeIdentity(keyId, Node.DEFAULT, IdentityService.DEFAULT)
                .thenCompose(identity -> {
                    this.identity = identity;
                    bisq1BridgeService.setIdentity(identity);
                    timestampService.setIdentity(identity);

                    authorizedOracleNode = createMyAuthorizedOracleNode();
                    bisq1BridgeService.setAuthorizedOracleNode(authorizedOracleNode);
                    timestampService.setAuthorizedOracleNode(authorizedOracleNode);

                    authorizedBondedRolesService.getAuthorizedDataSet().addListener(new CollectionObserver<>() {
                        @Override
                        public void add(AuthorizedData element) {
                        }

                        @Override
                        public void remove(Object element) {
                            if (element instanceof AuthorizedData) {
                                AuthorizedData authorizedData = (AuthorizedData) element;
                                if (authorizedData.getDistributedData() instanceof AuthorizedBondedRole) {
                                    networkService.removeAuthorizedData(authorizedData, identity.getNodeIdAndKeyPair());
                                }
                            }
                        }

                        @Override
                        public void clear() {
                        }
                    });

                    return bisq1BridgeService.initialize()
                            .thenCompose(result -> timestampService.initialize());
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (scheduler != null) {
            scheduler.stop();
        }
        if (startupScheduler != null) {
            startupScheduler.stop();
        }
        return bisq1BridgeService.shutdown()
                .thenCompose(result -> timestampService.shutdown());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private AuthorizedOracleNode createMyAuthorizedOracleNode() {
        AuthorizedOracleNode authorizedOracleNode = new AuthorizedOracleNode(identity.getNetworkId(), bondUserName, signatureBase64);
        // Repeat 3 times at startup to republish to ensure the data gets well distributed
        startupScheduler = Scheduler.run(() -> publishAuthorizedOracleNode(authorizedOracleNode, identity.getNodeIdAndKeyPair()))
                .repeated(1, 10, TimeUnit.SECONDS, 3);

        // We have 30 days TTL for the data, we republish after 25 days to ensure the data does not expire
        scheduler = Scheduler.run(() -> publishAuthorizedOracleNode(authorizedOracleNode, identity.getNodeIdAndKeyPair()))
                .periodically(25, TimeUnit.DAYS);

        return authorizedOracleNode;
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