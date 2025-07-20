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

import bisq.common.application.Service;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BlockData;
import bisq.oracle_node.bisq1_bridge.protobuf.BlockDataSubscription;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class Bisq1BridgeGrpcClientService implements Service {
    private final GrpcClient grpcClient;

    public Bisq1BridgeGrpcClientService() {
        grpcClient = new GrpcClient(50051);
    }

    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return grpcClient.initialize();
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return grpcClient.shutdown();
    }

    public void subscribeBlockData() {
        var subscription = BlockDataSubscription.newBuilder()
                .build();
        grpcClient.getAsyncStub().subscribeBlockData(subscription, new StreamObserver<>() {
            @Override
            public void onNext(bisq.oracle_node.bisq1_bridge.protobuf.BlockData proto) {
                BlockData blockData = BlockData.fromProto(proto);
                log.info("Received blockData: {}", blockData);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error at BlockDataSubscription", throwable);
            }

            @Override
            public void onCompleted() {
                log.info("BlockDataSubscription completed");
            }
        });
    }
}
