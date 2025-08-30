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

package bisq.network.i2p.grpc.server;

import bisq.bi2p.protobuf.Bi2pGrpc;
import bisq.bi2p.protobuf.NetworkState;
import bisq.bi2p.protobuf.NetworkStateUpdate;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
public final class Bi2pGrpcServerMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        Bi2pGrpc.Bi2pImplBase mockService = new Bi2pGrpc.Bi2pImplBase() {
            @Override
            public void subscribeNetworkState(bisq.bi2p.protobuf.SubscribeRequest request,
                                              StreamObserver<NetworkStateUpdate> responseObserver) {
                CompletableFuture.runAsync(() -> {
                    NetworkState[] states = NetworkState.values();
                    for (int i = 1; i < states.length; i++) {
                        NetworkState state = states[i];
                        var stateUpdate = NetworkStateUpdate.newBuilder()
                                .setValue(state)
                                .build();
                        responseObserver.onNext(stateUpdate);
                        try {
                            Thread.sleep(1000); // simulate delay
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    responseObserver.onCompleted();
                });
            }
        };
        Bi2pGrpcServer i2pGrpcServer = new Bi2pGrpcServer("127.0.0.1", 7777, mockService);
        i2pGrpcServer.initialize();
        keepRunning();
    }

    private static void keepRunning() {
        try {
            // Keep running
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted at keepRunning method", e);
            Thread.currentThread().interrupt(); // Restore interrupted state
        }
    }
}
