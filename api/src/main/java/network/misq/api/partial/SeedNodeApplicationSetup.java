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

package network.misq.api.partial;

import lombok.Getter;
import network.misq.application.ApplicationSetup;
import network.misq.application.options.ApplicationOptions;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.network.NetworkService;
import network.misq.network.NetworkServiceConfigFactory;
import network.misq.security.KeyPairRepository;
import network.misq.security.KeyPairRepositoryConfigFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Creates domain specific options from program arguments and application options.
 * Creates domain instance with options and optional dependency to other domain objects.
 * Initializes the domain instances according to the requirements of their dependencies either in sequence
 * or in parallel.
 * Provides the completely setup instances to other clients (Api)
 */
@Getter
public class SeedNodeApplicationSetup extends ApplicationSetup {
    private final KeyPairRepository keyPairRepository;
    private final NetworkService networkService;

    public SeedNodeApplicationSetup(ApplicationOptions applicationOptions, String[] args) {
        super("seed");

        KeyPairRepository.Conf keyPairRepositoryConf = new KeyPairRepositoryConfigFactory(applicationOptions.baseDir()).get();
        keyPairRepository = new KeyPairRepository(keyPairRepositoryConf);

        NetworkService.Config networkServiceConfig = new NetworkServiceConfigFactory(applicationOptions.baseDir(),
                getConfig("misq.networkServiceConfig"), args).get();
        networkService = new NetworkService(networkServiceConfig, keyPairRepository);
    }

    /**
     * Initializes all domain objects.
     * We do in parallel as far as possible. If there are dependencies we chain those as sequence.
     */
    public CompletableFuture<Boolean> initialize() {
        List<CompletableFuture<Boolean>> allFutures = new ArrayList<>();
        // Assuming identityRepository depends on keyPairRepository being initialized... 
        allFutures.add(keyPairRepository.initialize());
        allFutures.add(networkService.bootstrapAsync());
        // Once all have successfully completed our initialize is complete as well
        return CompletableFutureUtils.allOf(allFutures)
                .thenApply(success -> success.stream().allMatch(e -> e))
                .orTimeout(10, TimeUnit.SECONDS)
                .thenCompose(CompletableFuture::completedFuture);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        keyPairRepository.shutdown();
        return networkService.shutdown();
    }
}
