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

package bisq.seed_node;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.timer.Scheduler;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SeedNodeService implements Service {
    @Getter
    public static class Config {
        private final String privateKey;
        private final String publicKey;
        private final String bondUserName;
        private final String profileId;
        private final String signatureBase64;
        private final boolean staticPublicKeysProvided;

        public Config(String privateKey,
                      String publicKey,
                      String bondUserName,
                      String profileId,
                      String signatureBase64,
                      boolean staticPublicKeysProvided) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.bondUserName = bondUserName;
            this.profileId = profileId;
            this.signatureBase64 = signatureBase64;
            this.staticPublicKeysProvided = staticPublicKeysProvided;
        }

        public static SeedNodeService.Config from(com.typesafe.config.Config config) {
            return new SeedNodeService.Config(config.getString("privateKey"),
                    config.getString("publicKey"),
                    config.getString("bondUserName"),
                    config.getString("profileId"),
                    config.getString("signatureBase64"),
                    config.getBoolean("staticPublicKeysProvided"));
        }
    }

    private final NetworkService networkService;
    private final IdentityService identityService;
    private final Optional<Config> optionalConfig;
    @Nullable
    private Scheduler startupScheduler, scheduler;

    public SeedNodeService(Optional<Config> optionalConfig,
                           NetworkService networkService,
                           IdentityService identityService) {
        this.optionalConfig = optionalConfig;
        this.networkService = networkService;
        this.identityService = identityService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        optionalConfig.ifPresent(config -> {
            String privateKey = config.getPrivateKey();
            PrivateKey authorizedPrivateKey = KeyGeneration.getPrivateKeyFromHex(privateKey);
            PublicKey authorizedPublicKey = KeyGeneration.getPublicKeyFromHex(config.getPublicKey());

            NetworkId networkId = identityService.getOrCreateDefaultIdentity().getNetworkId();
            AuthorizedBondedRole authorizedBondedRole = new AuthorizedBondedRole(config.getProfileId(),
                    Hex.encode(authorizedPublicKey.getEncoded()),
                    BondedRoleType.SEED_NODE,
                    config.getBondUserName(),
                    config.getSignatureBase64(),
                    Optional.of(networkId.getAddressByTransportTypeMap()),
                    networkId,
                    Optional.empty(),
                    config.isStaticPublicKeysProvided());
            // The authorized key owns the authorized data which this node publishes (see
            // docs/specifications/network/storage-ownership.md)
            KeyPair authorizedKeyPair = new KeyPair(authorizedPublicKey, authorizedPrivateKey);
            // Seed nodes of earlier versions published their bonded role with the key pair of their default identity.
            // Applications use only data which its owner published, so we publish it again with the authorized key pair.
            KeyPair earlierKeyPair = identityService.getOrCreateDefaultIdentity().getNetworkIdWithKeyPair().getKeyPair();

            // Repeat 3 times at startup to republish to ensure the data gets well distributed
            startupScheduler = Scheduler.run(() -> {
                        networkService.migratePublisherOfAuthorizedData(earlierKeyPair, authorizedKeyPair);
                        publishMyBondedRole(authorizedBondedRole, authorizedKeyPair);
                    })
                    .host(this)
                    .runnableName("publishMyBondedRoleAtStartup")
                    .repeated(10, 60, TimeUnit.SECONDS, 3);

            // We have 100 days TTL for the data, we republish after 50 days to ensure the data does not expire
            scheduler = Scheduler.run(() -> publishMyBondedRole(authorizedBondedRole, authorizedKeyPair))
                    .host(this)
                    .runnableName("publishMyBondedRoleAfter50Days")
                    .periodically(50, TimeUnit.DAYS);
        });
        return CompletableFuture.completedFuture(true);
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
        return CompletableFuture.completedFuture(true);
    }

    private void publishMyBondedRole(AuthorizedBondedRole authorizedBondedRole, KeyPair authorizedKeyPair) {
        networkService.publishAuthorizedData(authorizedBondedRole, authorizedKeyPair);
    }
}