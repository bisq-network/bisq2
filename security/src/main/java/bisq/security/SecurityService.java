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

package bisq.security;

import bisq.common.application.Service;
import bisq.common.platform.OS;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundleService;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SecurityService implements Service {
    static {
        if (OS.isAndroid()) {
            // Androids default BC version does not support all algorithms we need, thus we remove
            // it and add our BC provider
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            Security.addProvider(new BouncyCastleProvider());
        } else if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Getter
    public static class Config {
        private final com.typesafe.config.Config keyBundle;

        public Config(com.typesafe.config.Config keyBundle) {
            this.keyBundle = keyBundle;
        }

        public static SecurityService.Config from(com.typesafe.config.Config config) {
            return new SecurityService.Config(config.getConfig("keyBundle"));
        }
    }

    @Getter
    private final KeyBundleService keyBundleService;
    @Getter
    private final HashCashProofOfWorkService hashCashProofOfWorkService;
    @Getter
    private final EquihashProofOfWorkService equihashProofOfWorkService;

    public SecurityService(PersistenceService persistenceService, Config config) {
        keyBundleService = new KeyBundleService(persistenceService, KeyBundleService.Config.from(config.getKeyBundle()));
        hashCashProofOfWorkService = new HashCashProofOfWorkService();
        equihashProofOfWorkService = new EquihashProofOfWorkService();
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return keyBundleService.initialize();
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }
}
