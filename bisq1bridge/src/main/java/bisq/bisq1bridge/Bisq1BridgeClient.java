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

package bisq.bisq1bridge;

import bisq.application.DefaultApplicationService;
import bisq.common.data.Pair;
import bisq.common.timer.Scheduler;
import bisq.network.NetworkService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.p2p.node.transport.Transport;
import bisq.oracle.daobridge.DaoBridgeService;
import bisq.oracle.daobridge.dto.ProofOfBurnDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Bisq1BridgeClient {
    private static final int DEFAULT_PORT = 8082;

    private final NetworkService networkService;
    private final DaoBridgeService daoBridgeService;
    private BaseHttpClient httpClient;
    private DefaultApplicationService applicationService;
    private AtomicInteger lastRequestedBlockHeight = new AtomicInteger(0);

    public Bisq1BridgeClient(String[] args) {
        if (args == null || args.length == 0) {
            args = new String[]{"--appName=Bisq1BridgeClient"};
        }
        applicationService = new DefaultApplicationService(args);
        applicationService.readAllPersisted().join();
        networkService = applicationService.getNetworkService();
        daoBridgeService = applicationService.getDaoBridgeService();

        applicationService.initialize().thenRun(() -> {
            httpClient = getHttpClient(DEFAULT_PORT);
            startRequests();
        });
    }

    private void startRequests() {
        requestProofOfBurnTxs();
        Scheduler.run(this::requestProofOfBurnTxs).periodically(5, TimeUnit.SECONDS);
    }

    private void requestProofOfBurnTxs() {
        CompletableFuture.supplyAsync(() -> {
                    try {
                        String path = "/api/v1/proof-of-burn/get-proof-of-burn/" + (lastRequestedBlockHeight.get() + 1);
                        log.info("Request Bisq DAO node: {}", path);
                        String response = httpClient.get(path,
                                Optional.of(new Pair<>("User-Agent", httpClient.userAgent)));
                        List<ProofOfBurnDto> proofOfBurnDtos = new ObjectMapper().readValue(response, new TypeReference<List<ProofOfBurnDto>>() {
                        });
                        log.info("Bisq DAO node response: {}", proofOfBurnDtos);
                        return proofOfBurnDtos;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new ArrayList<ProofOfBurnDto>();
                    }
                }, Executors.newSingleThreadExecutor())
                .thenApply(this::publishProofOfBurnDtoSet);
    }

    private CompletableFuture<Boolean> publishProofOfBurnDtoSet(List<ProofOfBurnDto> proofOfBurnDtoList) {
        if (proofOfBurnDtoList.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }
        lastRequestedBlockHeight.set(proofOfBurnDtoList.stream()
                .max(Comparator.comparingInt(ProofOfBurnDto::getBlockHeight))
                .map(ProofOfBurnDto::getBlockHeight)
                .orElse(0));
        return daoBridgeService.publishProofOfBurnDtoSet(proofOfBurnDtoList);
    }

    private BaseHttpClient getHttpClient(int port) {
        String userAgent = "Bisq1BridgeNode";
        String url = "http://localhost:" + port;
        return networkService.getHttpClient(url, userAgent, Transport.Type.CLEAR);
    }
}
