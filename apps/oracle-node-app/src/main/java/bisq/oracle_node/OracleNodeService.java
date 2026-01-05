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
import bisq.bonded_roles.market_price.MarketPriceRequestService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.platform.MemoryReportService;
import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.oracle_node.bisq1_bridge.Bisq1BridgeService;
import bisq.oracle_node.market_price.MarketPricePropagationService;
import bisq.oracle_node.timestamp.TimestampService;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import bisq.user.reputation.ReputationDataUtil;
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
        private final com.typesafe.config.Config bisq1Bridge;
        private final boolean staticPublicKeysProvided;

        public Config(String privateKey,
                      String publicKey,
                      boolean ignoreSecurityManager,
                      String bondUserName,
                      String profileId,
                      String signatureBase64,
                      boolean staticPublicKeysProvided,
                      com.typesafe.config.Config bisq1Bridge) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.ignoreSecurityManager = ignoreSecurityManager;
            this.bondUserName = bondUserName;
            this.profileId = profileId;
            this.signatureBase64 = signatureBase64;
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
                    config.getBoolean("staticPublicKeysProvided"),
                    config.getConfig("bisq1Bridge"));
        }
    }

    private final OracleNodeService.Config config;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final PersistenceService persistenceService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final MarketPricePropagationService marketPricePropagationService;

    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    private final TimestampService timestampService;
    private final String bondUserName;
    private final String signatureBase64;
    private final String profileId;
    private final boolean staticPublicKeysProvided;

    private Bisq1BridgeService bisq1BridgeService;
    @Nullable
    private Scheduler startupScheduler, scheduler;
    @Nullable
    private Pin bondedRolesPin;

    public OracleNodeService(Config config,
                             PersistenceService persistenceService,
                             NetworkService networkService,
                             IdentityService identityService,
                             AuthorizedBondedRolesService authorizedBondedRolesService,
                             MarketPriceRequestService marketPriceRequestService,
                             MemoryReportService memoryReportService) {
        this.config = config;
        this.identityService = identityService;
        this.networkService = networkService;
        this.persistenceService = persistenceService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;

        bondUserName = config.getBondUserName();
        signatureBase64 = config.getSignatureBase64();
        staticPublicKeysProvided = config.isStaticPublicKeysProvided();
        profileId = config.getProfileId();

        boolean ignoreSecurityManager = config.isIgnoreSecurityManager();
        checkArgument(StringUtils.isNotEmpty(bondUserName));
        checkArgument(StringUtils.isNotEmpty(signatureBase64));
        checkArgument(StringUtils.isNotEmpty(profileId));

        String privateKey = config.getPrivateKey();
        String publicKey = config.getPublicKey();
        checkArgument(StringUtils.isNotEmpty(privateKey));
        checkArgument(StringUtils.isNotEmpty(publicKey));

        authorizedPrivateKey = KeyGeneration.getPrivateKeyFromHex(privateKey);
        authorizedPublicKey = KeyGeneration.getPublicKeyFromHex(publicKey);

        timestampService = new TimestampService(persistenceService,
                identityService,
                networkService,
                authorizedBondedRolesService,
                authorizedPrivateKey,
                authorizedPublicKey,
                staticPublicKeysProvided);

        marketPricePropagationService = new MarketPricePropagationService(identityService,
                networkService,
                marketPriceRequestService,
                authorizedPrivateKey,
                authorizedPublicKey,
                staticPublicKeysProvided);
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        ReputationDataUtil.cleanupMap(networkService);

        Identity identity = identityService.getOrCreateDefaultIdentity();
        NetworkId networkId = identity.getNetworkId();
        KeyPair keyPair = identity.getNetworkIdWithKeyPair().getKeyPair();
        byte[] authorizedPublicKeyEncoded = authorizedPublicKey.getEncoded();
        String authorizedPublicKeyAsHex = Hex.encode(authorizedPublicKeyEncoded);
        AuthorizedOracleNode myAuthorizedOracleNode = new AuthorizedOracleNode(networkId,
                profileId,
                authorizedPublicKeyAsHex,
                bondUserName,
                signatureBase64,
                staticPublicKeysProvided);

        Bisq1BridgeService.Config bisq1BridgeConfig = Bisq1BridgeService.Config.from(config.getBisq1Bridge());
        bisq1BridgeService = new Bisq1BridgeService(bisq1BridgeConfig,
                persistenceService,
                identityService,
                networkService,
                authorizedBondedRolesService,
                authorizedPrivateKey,
                authorizedPublicKey,
                staticPublicKeysProvided,
                myAuthorizedOracleNode);

        // We only self-publish if we are a root oracle
        if (staticPublicKeysProvided) {
            AuthorizedBondedRole authorizedBondedRole = new AuthorizedBondedRole(profileId,
                    authorizedPublicKeyAsHex,
                    BondedRoleType.ORACLE_NODE,
                    bondUserName,
                    signatureBase64,
                    Optional.of(identityService.getOrCreateDefaultIdentity().getNetworkId().getAddressByTransportTypeMap()),
                    networkId,
                    Optional.of(myAuthorizedOracleNode),
                    true);

            // Repeat 3 times at startup to republish to ensure the data gets well distributed
            startupScheduler = Scheduler.run(() -> publishMyAuthorizedData(myAuthorizedOracleNode, authorizedBondedRole, keyPair))
                    .host(this)
                    .runnableName("publishMyAuthorizedDataAtStartup")
                    .repeated(10, 60, TimeUnit.SECONDS, 3);

            // We have 100 days TTL for the data, we republish after 50 days to ensure the data does not expire
            scheduler = Scheduler.run(() -> {
                        bisq1BridgeService.republishAuthorizedBondedRoles();
                        publishMyAuthorizedData(myAuthorizedOracleNode, authorizedBondedRole, keyPair);
                    })
                    .host(this)
                    .runnableName("publishMyAuthorizedDataAfter50Days")
                    .periodically(50, TimeUnit.DAYS);
        }

        bondedRolesPin = authorizedBondedRolesService.getBondedRoles().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BondedRole element) {
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BondedRole bondedRole) {
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
                .thenCompose(result -> timestampService.initialize())
                .thenCompose(result -> marketPricePropagationService.initialize())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Initialisation failed", throwable);
                    } else if (!result) {
                        log.error("Initialisation failed");
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
        if (startupScheduler != null) {
            startupScheduler.stop();
            startupScheduler = null;
        }

        if (bondedRolesPin != null) {
            bondedRolesPin.unbind();
            bondedRolesPin = null;
        }

        if (bisq1BridgeService != null) {
            return bisq1BridgeService.shutdown()
                    .thenCompose(result -> timestampService.shutdown())
                    .thenCompose(result -> marketPricePropagationService.shutdown());
        } else {
            return timestampService.shutdown()
                    .thenCompose(result -> marketPricePropagationService.shutdown());
        }
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

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