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
import bisq.common.observable.collection.ObservableArray;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BlockDto;
import bisq.oracle_node.bisq1_bridge.protobuf.BlockSubscription;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class Bisq1BridgeGrpcClientService implements Service {
    @Getter
    @ToString
    public static final class Config {
        private final int port;

        public Config(int port) {
            this.port = port;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getInt("port"));
        }
    }

    private final GrpcClient grpcClient;
    @Getter
    private final ObservableArray<BlockDto> blockDtoList = new ObservableArray<>();

    public Bisq1BridgeGrpcClientService(Config config) {
        grpcClient = new GrpcClient(config.getPort());
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

    public void subscribeBlockUpdate() {
        var subscription = BlockSubscription.newBuilder()
                .build();
        grpcClient.getAsyncStub().subscribeBlockUpdate(subscription, new StreamObserver<>() {
            @Override
            public void onNext(bisq.oracle_node.bisq1_bridge.protobuf.BlockDto proto) {
                BlockDto blockDto = BlockDto.fromProto(proto);
                blockDtoList.add(blockDto);
                log.info("Received blockDto: {}", blockDto);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error at BlockDtoSubscription", throwable);
            }

            @Override
            public void onCompleted() {
                log.info("BlockDtoSubscription completed");
            }
        });
    }
}
