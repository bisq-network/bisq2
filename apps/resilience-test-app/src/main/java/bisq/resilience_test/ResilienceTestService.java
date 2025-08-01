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

package bisq.resilience_test;

import bisq.common.application.Service;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.resilience_test.test.BaseTestCase;
import bisq.resilience_test.test.ConnectionBurstTestCase;
import bisq.resilience_test.test.MessageBurstTestCase;
import bisq.user.reputation.ReputationDataUtil;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ResilienceTestService implements Service {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final Optional<Config> optionalConfig;
    private List<BaseTestCase> testCases = new ArrayList<>();

    public ResilienceTestService(Optional<Config> optionalConfig,
                                 NetworkService networkService,
                                 IdentityService identityService) {
        this.optionalConfig = optionalConfig;
        this.networkService = networkService;
        this.identityService = identityService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        ReputationDataUtil.cleanupMap(networkService);

        if (optionalConfig.isPresent()) {
            var config = optionalConfig.get();
            testCases.add(new MessageBurstTestCase(getConfig(config, "messageBurst"), networkService, identityService));
            testCases.add(new ConnectionBurstTestCase(getConfig(config, "connectionBurst"), networkService, identityService));
        }
        if (testCases.isEmpty() || testCases.stream().noneMatch(BaseTestCase::isEnabled)) {
            return getInvalidConfigFuture();
        } else {
            testCases.stream().filter(BaseTestCase::isEnabled).forEach(BaseTestCase::start);
        }
        return CompletableFuture.completedFuture(true);
    }


    private Optional<Config> getConfig(Config config, String path) {
        if (config.hasPath(path)) {
            return Optional.of(config.getConfig(path));
        }
        return Optional.empty();
    }

    private CompletableFuture<Boolean> getInvalidConfigFuture() {
        return CompletableFuture.failedFuture(new RuntimeException("No resilience test settings were enabled. shutting down..."));
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        CompletableFuture<?>[] futures = testCases.stream()
                .map(BaseTestCase::shutdown)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures).handle((ignored, ex) -> true);
    }

}