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

package bisq.network.i2p.grpc.client;

import bisq.common.application.Service;
import bisq.i2p.protobuf.I2pRouterMonitorGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcRouterMonitorClient implements Service {
    private ManagedChannel managedChannel;
    @Getter
    private I2pRouterMonitorGrpc.I2pRouterMonitorStub stub;
    private final String host;
    private final int port;

    public GrpcRouterMonitorClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (managedChannel == null) {
            managedChannel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .build();
        }
        stub = I2pRouterMonitorGrpc.newStub(managedChannel);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (managedChannel != null) {
            managedChannel.shutdown();
            try {
                if (!managedChannel.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    managedChannel.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Thread got interrupted at while shutting down I2pRouterMonitorGrpcClient", e);
                Thread.currentThread().interrupt(); // Restore interrupted state

                managedChannel.shutdownNow();
            }
            managedChannel = null;
            stub = null;
        }
        return CompletableFuture.completedFuture(true);
    }
}
