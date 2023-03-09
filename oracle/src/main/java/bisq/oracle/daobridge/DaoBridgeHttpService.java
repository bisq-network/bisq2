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

package bisq.oracle.daobridge;

import bisq.common.application.DevMode;
import bisq.common.application.Service;
import bisq.common.data.Pair;
import bisq.common.threading.ExecutorFactory;
import bisq.network.NetworkService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.p2p.node.transport.Transport;
import bisq.oracle.daobridge.dto.BondedReputationDto;
import bisq.oracle.daobridge.dto.ProofOfBurnDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DaoBridgeHttpService implements Service {
    //todo set it to block height of launch date. We are not interested in txs published before
    private static final int LAUNCH_BLOCK_HEIGHT = 0;

    @Getter
    @ToString
    public static final class Config {
        private final String url;

        public Config(String url) {
            this.url = url;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getString("url"));
        }
    }

    private final ExecutorService executorService = ExecutorFactory.newCachedThreadPool("dao-bridge-http-thread", 4, 60);
    private final AtomicInteger lastRequestedProofOfBurnBlockHeight = new AtomicInteger(0);
    private final AtomicInteger lastRequestedBondedReputationBlockHeight = new AtomicInteger(0);
    private final NetworkService networkService;
    private final String url;
    private BaseHttpClient httpClient;

    public DaoBridgeHttpService(Config config, NetworkService networkService) {
        this.networkService = networkService;
        this.url = config.getUrl();

        if (!DevMode.isDevMode()) {
            lastRequestedProofOfBurnBlockHeight.set(LAUNCH_BLOCK_HEIGHT);
            lastRequestedBondedReputationBlockHeight.set(LAUNCH_BLOCK_HEIGHT);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        // We expect to request at localhost, so we use clear net 
        httpClient = networkService.getHttpClient(url, "DaoBridgeService", Transport.Type.CLEAR);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        executorService.shutdownNow();
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<List<ProofOfBurnDto>> requestProofOfBurnTxs() {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        String path = "/api/v1/proof-of-burn/get-proof-of-burn/" + (lastRequestedProofOfBurnBlockHeight.get() + 1);
                        log.info("Request Bisq DAO node: {}", path);
                        String response = httpClient.get(path,
                                Optional.of(new Pair<>("User-Agent", httpClient.userAgent)));
                        List<ProofOfBurnDto> dtoList = new ObjectMapper().readValue(response, new TypeReference<>() {
                        });
                        log.info("Bisq DAO node response: {}", dtoList);
                        return dtoList;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new ArrayList<ProofOfBurnDto>();
                    }
                }, executorService)
                .whenComplete((list, throwable) -> {
                    if (throwable == null && !list.isEmpty()) {
                        lastRequestedProofOfBurnBlockHeight.set(list.stream()
                                .max(Comparator.comparingInt(ProofOfBurnDto::getBlockHeight))
                                .map(ProofOfBurnDto::getBlockHeight)
                                .orElse(0));
                    }
                });
    }

    public CompletableFuture<List<BondedReputationDto>> requestBondedReputations() {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        String path = "/api/v1/bonded-reputation/get-bonded-reputation/" + (lastRequestedBondedReputationBlockHeight.get() + 1);
                        log.info("Request Bisq DAO node: {}", path);
                        String response = httpClient.get(path,
                                Optional.of(new Pair<>("User-Agent", httpClient.userAgent)));
                        List<BondedReputationDto> dtoList = new ObjectMapper().readValue(response, new TypeReference<>() {
                        });
                        log.info("Bisq DAO node response: {}", dtoList);
                        return dtoList;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new ArrayList<BondedReputationDto>();
                    }
                }, executorService)
                .whenComplete((list, throwable) -> {
                    if (throwable == null && !list.isEmpty()) {
                        lastRequestedBondedReputationBlockHeight.set(list.stream()
                                .max(Comparator.comparingInt(BondedReputationDto::getBlockHeight))
                                .map(BondedReputationDto::getBlockHeight)
                                .orElse(0));
                    }
                });
    }

    public CompletableFuture<Optional<Long>> requestAccountAgeWitness(String hashAsHex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String path = "/api/v1/account-age/get-date/" + hashAsHex;
                log.info("Request account age witness: {}", path);
                String response = httpClient.get(path,
                        Optional.of(new Pair<>("User-Agent", httpClient.userAgent)));
                Long date = new ObjectMapper().readValue(response, new TypeReference<>() {
                });
                log.info("Bisq DAO node response: {}", date);
                return Optional.of(date);
            } catch (IOException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }, executorService);
    }

    public CompletableFuture<Optional<Long>> requestSignedWitnessDate(String hashAsHex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String path = "/api/v1/signed-witness/get-date/" + hashAsHex;
                log.info("Request signed-witness: {}", path);
                String response = httpClient.get(path,
                        Optional.of(new Pair<>("User-Agent", httpClient.userAgent)));
                Long date = new ObjectMapper().readValue(response, new TypeReference<>() {
                });
                log.info("Bisq DAO node response: {}", date);
                return Optional.of(date);
            } catch (IOException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }, executorService);
    }
}