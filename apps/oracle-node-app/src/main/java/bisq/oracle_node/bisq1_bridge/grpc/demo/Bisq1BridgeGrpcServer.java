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

package bisq.oracle_node.bisq1_bridge.grpc.demo;

import bisq.common.application.Service;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BlockDto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BurningManDto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.TxDto;
import bisq.oracle_node.bisq1_bridge.protobuf.Bisq1BridgeServiceGrpc;
import bisq.oracle_node.bisq1_bridge.protobuf.BlockSubscription;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class Bisq1BridgeGrpcServer implements Service {
    private Server server;

    public Bisq1BridgeGrpcServer(int port) {
        server = ServerBuilder
                .forPort(port)
                .addService(new Bisq1BridgeServiceGrpcImpl())
                .build();
        System.out.println("Starting gRPC server on port " + port);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        try {
            server.start();
            System.out.println("Server started.");
            server.awaitTermination();
            return CompletableFuture.completedFuture(true);
        } catch (IOException | InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        server.shutdown();
        return CompletableFuture.completedFuture(true);
    }

    static class Bisq1BridgeServiceGrpcImpl extends Bisq1BridgeServiceGrpc.Bisq1BridgeServiceImplBase {
        public void subscribeBlockData(BlockSubscription subscription,
                                       StreamObserver<bisq.oracle_node.bisq1_bridge.protobuf.BlockDto> streamObserver) {
            CompletableFuture.runAsync(() -> {
                for (int i = 0; i <= 3; i++) {
                    int height = i;
                    byte[] hash = ("" + i).getBytes();
                    long time = i * 10000;
                    List<TxDto> txDataList = new ArrayList<>();
                    List<BurningManDto> burningManDataList = new ArrayList<>();

                    BlockDto blockData = new BlockDto(height,
                            hash,
                            time,
                            txDataList,
                            burningManDataList);
                    bisq.oracle_node.bisq1_bridge.protobuf.BlockDto response = blockData.getBuilder(true).build();
                    log.info(" observers with new block {}", blockData);
                    streamObserver.onNext(response);
                    try {
                        Thread.sleep(200); // simulate delay
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                streamObserver.onCompleted();
                log.info("onCompleted");
            });
        }
    }
}
