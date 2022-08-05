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
import bisq.common.timer.Scheduler;
import bisq.oracle.daobridge.DaoBridgeHttpService;
import bisq.oracle.daobridge.DaoBridgeService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    private final DaoBridgeHttpService daoBridgeHttpService;

    public Bisq1BridgeClient(String[] args) {
        BridgeApplicationService applicationService = new BridgeApplicationService(args);
        daoBridgeHttpService = applicationService.getDaoBridgeHttpService();
        daoBridgeService = applicationService.getDaoBridgeService();

        applicationService.readAllPersisted()
                .thenCompose(result -> applicationService.initialize())
                .thenRun(this::startRequests);
    }

    private void startRequests() {
        Scheduler.run(this::requestSerial).periodically(0, 5, TimeUnit.SECONDS);
    }

    private CompletableFuture<Boolean> requestSerial() {
        return daoBridgeHttpService.requestProofOfBurnTxs()
                .thenCompose(daoBridgeService::publishProofOfBurnDtoSet)
                .thenCompose(result -> daoBridgeHttpService.requestBondedReputations())
                .thenCompose(daoBridgeService::publishBondedReputationDtoSet);
    }
}
