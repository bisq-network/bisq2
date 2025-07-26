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

package bisq.oracle_node.bisq1_bridge.grpc;

import bisq.bridge.protobuf.AccountAgeWitnessGrpcServiceGrpc;
import bisq.bridge.protobuf.BondedRoleGrpcServiceGrpc;
import bisq.bridge.protobuf.BsqBlockGrpcServiceGrpc;
import bisq.bridge.protobuf.BurningmanGrpcServiceGrpc;
import bisq.bridge.protobuf.SignedWitnessGrpcServiceGrpc;
import bisq.common.application.Service;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcClient implements Service {
    private final String host;
    private final int port;
    private ManagedChannel managedChannel;

    @Getter
    private BsqBlockGrpcServiceGrpc.BsqBlockGrpcServiceStub bsqBlockGrpcService;
    @Getter
    private BurningmanGrpcServiceGrpc.BurningmanGrpcServiceStub burningmanGrpcService;
    @Getter
    private AccountAgeWitnessGrpcServiceGrpc.AccountAgeWitnessGrpcServiceBlockingStub accountAgeWitnessGrpcService;
    @Getter
    private SignedWitnessGrpcServiceGrpc.SignedWitnessGrpcServiceBlockingStub signedWitnessGrpcService;
    @Getter
    private BondedRoleGrpcServiceGrpc.BondedRoleGrpcServiceBlockingStub bondedRoleGrpcService;

    public GrpcClient(int port) {
        this("localhost", port);
    }

    public GrpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        managedChannel = Grpc.newChannelBuilderForAddress(
                        host,
                        port,
                        InsecureChannelCredentials.create())
                .enableRetry()
                .build();

        try {
            bsqBlockGrpcService = BsqBlockGrpcServiceGrpc.newStub(managedChannel);
            burningmanGrpcService = BurningmanGrpcServiceGrpc.newStub(managedChannel);
            accountAgeWitnessGrpcService = AccountAgeWitnessGrpcServiceGrpc.newBlockingStub(managedChannel);
            signedWitnessGrpcService = SignedWitnessGrpcServiceGrpc.newBlockingStub(managedChannel);
            bondedRoleGrpcService = BondedRoleGrpcServiceGrpc.newBlockingStub(managedChannel);
        } catch (Exception e) {
            log.error("Initializing grpc client failed", e);
            dispose();
            throw e;
        }

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        dispose();
        return CompletableFuture.completedFuture(true);
    }

    private void dispose() {
        if (managedChannel != null) {
            managedChannel.shutdown();
            try {
                if (!managedChannel.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    managedChannel.shutdownNow();
                }
            } catch (InterruptedException e) {
                managedChannel.shutdownNow();
                Thread.currentThread().interrupt();
            }
            managedChannel = null;
        }
        bsqBlockGrpcService = null;
        burningmanGrpcService = null;
        accountAgeWitnessGrpcService = null;
        signedWitnessGrpcService = null;
        bondedRoleGrpcService = null;
    }
}
