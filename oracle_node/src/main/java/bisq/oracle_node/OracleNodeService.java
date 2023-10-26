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

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.Node;
import bisq.oracle_node.bisq1_bridge.Bisq1BridgeService;
import bisq.oracle_node.timestamp.TimestampService;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.KeyGeneration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class OracleNodeService implements Service {
    @Getter
    public static class Config {
        private final String privateKey;
        private final String publicKey;
        private final boolean ignoreSecurityManager;
        private final String bondUserName;
        private final String profileId;
        private final String signatureBase64;
        private final String keyId;
        private final com.typesafe.config.Config bisq1Bridge;
        private final boolean staticPublicKeysProvided;

        public Config(String privateKey,
                      String publicKey,
                      boolean ignoreSecurityManager,
                      String bondUserName,
                      String profileId,
                      String signatureBase64,
                      String keyId,
                      boolean staticPublicKeysProvided,
                      com.typesafe.config.Config bisq1Bridge) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.ignoreSecurityManager = ignoreSecurityManager;
            this.bondUserName = bondUserName;
            this.profileId = profileId;
            this.signatureBase64 = signatureBase64;
            this.keyId = keyId;
            this.staticPublicKeysProvided = staticPublicKeysProvided;
            this.bisq1Bridge = bisq1Bridge;
        }

        public static OracleNodeService.Config from(com.typesafe.config.Config config) {
            return new OracleNodeService.Config(config.getString("privateKey"),
                    config.getString("publicKey"),
                    config.getBoolean("ignoreSecurityManager"),
                    config.getString("bondUserName"),
                    config.getString("profileId"),
                    config.getString("signatureBase64"),
                    config.getString("keyId"),
                    config.getBoolean("staticPublicKeysProvided"),
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
    private final String profileId;
    private final boolean staticPublicKeysProvided;
    private AuthorizedBondedRole authorizedBondedRole;
    private AuthorizedOracleNode authorizedOracleNode;
    @Nullable
    private Scheduler startupScheduler, scheduler;

    public OracleNodeService(Config config,
                             IdentityService identityService,
                             NetworkService networkService,
                             PersistenceService persistenceService,
                             AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.identityService = identityService;
        this.networkService = networkService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;

        bondUserName = config.getBondUserName();
        signatureBase64 = config.getSignatureBase64();
        keyId = config.getKeyId();
        staticPublicKeysProvided = config.isStaticPublicKeysProvided();
        profileId = config.getProfileId();

        boolean ignoreSecurityManager = config.isIgnoreSecurityManager();
        checkArgument(StringUtils.isNotEmpty(bondUserName));
        checkArgument(StringUtils.isNotEmpty(signatureBase64));
        checkArgument(StringUtils.isNotEmpty(keyId));
        checkArgument(StringUtils.isNotEmpty(profileId));

        String privateKey = config.getPrivateKey();
        String publicKey = config.getPublicKey();
        checkArgument(StringUtils.isNotEmpty(privateKey));
        checkArgument(StringUtils.isNotEmpty(publicKey));

        authorizedPrivateKey = KeyGeneration.getPrivateKeyFromHex(privateKey);
        authorizedPublicKey = KeyGeneration.getPublicKeyFromHex(publicKey);

        Bisq1BridgeService.Config bisq1BridgeConfig = Bisq1BridgeService.Config.from(config.getBisq1Bridge());
        bisq1BridgeService = new Bisq1BridgeService(bisq1BridgeConfig,
                networkService,
                persistenceService,
                authorizedBondedRolesService,
                authorizedPrivateKey,
                authorizedPublicKey,
                ignoreSecurityManager,
                staticPublicKeysProvided);

        timestampService = new TimestampService(persistenceService,
                networkService,
                authorizedPrivateKey,
                authorizedPublicKey,
                staticPublicKeysProvided);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        return identityService.createAndInitializeIdentity(keyId, Node.DEFAULT, IdentityService.DEFAULT)
                .thenCompose(identity -> {
                    bisq1BridgeService.setIdentity(identity);
                    timestampService.setIdentity(identity);

                    String publicKeyHash = Hex.encode(DigestUtil.hash(authorizedPublicKey.getEncoded()));
                    NetworkId networkId = identity.getNetworkId();
                    authorizedOracleNode = new AuthorizedOracleNode(networkId, bondUserName, signatureBase64, publicKeyHash, staticPublicKeysProvided);
                    bisq1BridgeService.setAuthorizedOracleNode(authorizedOracleNode);
                    timestampService.setAuthorizedOracleNode(authorizedOracleNode);

                    Optional<AuthorizedOracleNode> oracleNode = staticPublicKeysProvided ? Optional.of(authorizedOracleNode) : Optional.empty();
                    authorizedBondedRole = new AuthorizedBondedRole(profileId,
                            Hex.encode(authorizedPublicKey.getEncoded()),
                            BondedRoleType.ORACLE_NODE,
                            bondUserName,
                            signatureBase64,
                            networkService.getAddressByTransportTypeMap(Node.DEFAULT),
                            networkId,
                            oracleNode,
                            staticPublicKeysProvided);

                    KeyPair keyPair = identity.getNodeIdAndKeyPair().getKeyPair();

                    // Repeat 3 times at startup to republish to ensure the data gets well distributed
                    startupScheduler = Scheduler.run(() -> publishMyAuthorizedData(authorizedOracleNode, authorizedBondedRole, keyPair))
                            .repeated(1, 10, TimeUnit.SECONDS, 3);

                    // We have 30 days TTL for the data, we republish after 25 days to ensure the data does not expire
                    scheduler = Scheduler.run(() -> publishMyAuthorizedData(authorizedOracleNode, authorizedBondedRole, keyPair))
                            .periodically(25, TimeUnit.DAYS);

                    authorizedBondedRolesService.getBondedRoles().addObserver(new CollectionObserver<>() {
                        @Override
                        public void add(BondedRole element) {
                        }

                        @Override
                        public void remove(Object element) {
                            if (element instanceof BondedRole) {
                                BondedRole bondedRole = (BondedRole) element;
                                networkService.removeAuthorizedData(bondedRole.getAuthorizedBondedRole(),
                                        keyPair,
                                        authorizedPublicKey);
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

    private void publishMyAuthorizedData(AuthorizedOracleNode authorizedOracleNode,
                                         AuthorizedBondedRole authorizedBondedRole,
                                         KeyPair keyPair) {
        networkService.publishAuthorizedData(authorizedBondedRole,
                keyPair,
                authorizedPrivateKey,
                authorizedPublicKey);
        networkService.publishAuthorizedData(authorizedOracleNode,
                keyPair,
                authorizedPrivateKey,
                authorizedPublicKey);
    }
}