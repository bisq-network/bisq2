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

package bisq.bisq1Bridge;

import bisq.application.BridgeApplicationService;
import bisq.common.data.Pair;
import bisq.common.threading.ExecutorFactory;
import bisq.common.timer.Scheduler;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.p2p.node.transport.Transport;
import bisq.oracle.daobridge.DaoBridgeService;
import bisq.oracle.daobridge.dto.BondedReputationDto;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client node to request on regular interval from a Bisq1 DAO node the proof of work data via the REST api.
 * When data are received it gets published to the Bisq2 P2P network.
 * As we use authorized data the operator need to provide the private/public keys which gives the permission to add
 * those data to the network.
 * Expected to run on the same server as the Bisq1 DAO node.
 */
@Slf4j
public class Bisq1BridgeClient {
    private final DaoBridgeService daoBridgeService;
    private final BaseHttpClient httpClient;
    private final AtomicInteger lastRequestedProofOfBurnBlockHeight = new AtomicInteger(0);
    private final AtomicInteger lastRequestedBondedReputationBlockHeight = new AtomicInteger(0);

    public Bisq1BridgeClient(String[] args) {
        BridgeApplicationService applicationService = new BridgeApplicationService(args);
        daoBridgeService = applicationService.getDaoBridgeService();

        String url = daoBridgeService.getConfig().getUrl();
        // We expect to request at localhost, so we use clear net 
        httpClient = applicationService.getNetworkService().getHttpClient(url, "Bisq1BridgeNode", Transport.Type.CLEAR);

        applicationService.readAllPersisted()
                .thenCompose(result -> applicationService.initialize())
                .thenRun(this::startRequests);
    }

    private void startRequests() {
        requestSerial().thenApply(result ->
                Scheduler.run(this::requestSerial).periodically(5, TimeUnit.SECONDS)
        );
    }

    private CompletableFuture<Boolean> requestSerial() {
        return requestProofOfBurnTxs()
                .thenCompose(result -> requestBondedReputations());
    }

    private CompletableFuture<Boolean> requestProofOfBurnTxs() {
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
                }, ExecutorFactory.newSingleThreadExecutor("Request-proof-of-burn-thread"))
                .thenCompose(list -> {
                    if (!list.isEmpty()) {
                        lastRequestedProofOfBurnBlockHeight.set(list.stream()
                                .max(Comparator.comparingInt(ProofOfBurnDto::getBlockHeight))
                                .map(ProofOfBurnDto::getBlockHeight)
                                .orElse(0));
                        return daoBridgeService.publishProofOfBurnDtoSet(list);
                    }
                    return CompletableFuture.completedFuture(false);
                });
    }

    private CompletableFuture<Boolean> requestBondedReputations() {
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
                }, ExecutorFactory.newSingleThreadExecutor("Request-bonded-reputation-thread"))
                .thenCompose(list -> {
                    if (!list.isEmpty()) {
                        lastRequestedBondedReputationBlockHeight.set(list.stream()
                                .max(Comparator.comparingInt(BondedReputationDto::getBlockHeight))
                                .map(BondedReputationDto::getBlockHeight)
                                .orElse(0));
                        return daoBridgeService.publishBondedReputationDtoSet(list);
                    }
                    return CompletableFuture.completedFuture(false);
                });
    }
}
