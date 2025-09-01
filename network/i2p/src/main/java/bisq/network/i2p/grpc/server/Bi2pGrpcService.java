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
import bisq.bi2p.protobuf.RouterInfoRequest;
import bisq.bi2p.protobuf.RouterInfoResponse;
import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.network.i2p.grpc.messages.NetworkStateUpdate;
import bisq.network.i2p.grpc.messages.ProcessStateUpdate;
import bisq.network.i2p.grpc.messages.RouterStateUpdate;
import bisq.network.i2p.grpc.messages.TunnelInfoUpdate;
import bisq.network.i2p.router.I2PRouter;
import bisq.network.i2p.router.state.RouterMonitor;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class Bi2pGrpcService extends Bi2pGrpc.Bi2pImplBase implements Service {
    private final Set<StreamObserver<bisq.bi2p.protobuf.ProcessStateUpdate>> processStateObservers = new CopyOnWriteArraySet<>();
    private final Set<StreamObserver<bisq.bi2p.protobuf.NetworkStateUpdate>> networkStateObservers = new CopyOnWriteArraySet<>();
    private final Set<StreamObserver<bisq.bi2p.protobuf.RouterStateUpdate>> routerStateObservers = new CopyOnWriteArraySet<>();
    private final Set<StreamObserver<bisq.bi2p.protobuf.TunnelInfoUpdate>> tunnelInfoObservers = new CopyOnWriteArraySet<>();
    private final Set<Pin> pins = new HashSet<>();
    private final RouterMonitor routerMonitor;

    public Bi2pGrpcService(I2PRouter i2pRouter) {
        routerMonitor = i2pRouter.getRouterMonitor();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        observeProcessState();
        observeNetworkState();
        observeRouterState();
        observeTunnelInfo();
        log.info("GrpcRouterMonitorService initialized");
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        pins.forEach(Pin::unbind);
        pins.clear();
        processStateObservers.clear();
        networkStateObservers.clear();
        routerStateObservers.clear();
        tunnelInfoObservers.clear();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void requestRouterInfo(RouterInfoRequest request,
                                  StreamObserver<RouterInfoResponse> responseObserver) {
        try {
            RouterInfoResponse response = RouterInfoResponse.newBuilder()
                    .setProcessState(routerMonitor.getProcessState().get().toProtoEnum())
                    .setNetworkState(routerMonitor.getNetworkState().get().toProtoEnum())
                    .setRouterState(routerMonitor.getRouterState().get().toProtoEnum())
                    .setTunnelInfo(routerMonitor.getTunnelInfo().get().completeProto())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void subscribeProcessState(bisq.bi2p.protobuf.SubscribeRequest request,
                                      StreamObserver<bisq.bi2p.protobuf.ProcessStateUpdate> responseObserver) {
        log.info("Subscribe for {}", request.getTopic());
        processStateObservers.add(responseObserver);
        ((ServerCallStreamObserver<bisq.bi2p.protobuf.ProcessStateUpdate>) responseObserver).setOnCancelHandler(() -> {
            log.info("Client for topic {} disconnected", request.getTopic());
            processStateObservers.remove(responseObserver);
        });
    }

    @Override
    public void subscribeNetworkState(bisq.bi2p.protobuf.SubscribeRequest request,
                                      StreamObserver<bisq.bi2p.protobuf.NetworkStateUpdate> responseObserver) {
        log.info("Subscribe for {}", request.getTopic());
        networkStateObservers.add(responseObserver);
        ((ServerCallStreamObserver<bisq.bi2p.protobuf.NetworkStateUpdate>) responseObserver).setOnCancelHandler(() -> {
            log.info("Client for topic {} disconnected", request.getTopic());
            networkStateObservers.remove(responseObserver);
        });
    }

    @Override
    public void subscribeRouterState(bisq.bi2p.protobuf.SubscribeRequest request,
                                     StreamObserver<bisq.bi2p.protobuf.RouterStateUpdate> responseObserver) {
        log.info("Subscribe for {}", request.getTopic());
        routerStateObservers.add(responseObserver);
        ((ServerCallStreamObserver<bisq.bi2p.protobuf.RouterStateUpdate>) responseObserver).setOnCancelHandler(() -> {
            log.info("Client for topic {} disconnected", request.getTopic());
            routerStateObservers.remove(responseObserver);
        });
    }

    @Override
    public void subscribeTunnelInfo(bisq.bi2p.protobuf.SubscribeRequest request,
                                    StreamObserver<bisq.bi2p.protobuf.TunnelInfoUpdate> responseObserver) {
        log.info("Subscribe for {}", request.getTopic());
        tunnelInfoObservers.add(responseObserver);
        ((ServerCallStreamObserver<bisq.bi2p.protobuf.TunnelInfoUpdate>) responseObserver).setOnCancelHandler(() -> {
            log.info("Client for topic {} disconnected", request.getTopic());
            tunnelInfoObservers.remove(responseObserver);
        });
    }

    private void observeProcessState() {
        var pin = routerMonitor.getProcessState().addObserver(value -> {
            if (value == null || processStateObservers.isEmpty()) {
                return;
            }
            ProcessStateUpdate stateUpdate = new ProcessStateUpdate(value);
            bisq.bi2p.protobuf.ProcessStateUpdate proto = stateUpdate.completeProto();
            processStateObservers.forEach(observer -> {
                try {
                    observer.onNext(proto);
                } catch (Exception e) {
                    log.error("Error at notifying grpc observer {}", observer, e);
                }
            });
        });
        pins.add(pin);
    }

    private void observeNetworkState() {
        var pin = routerMonitor.getNetworkState().addObserver(value -> {
            if (value == null || networkStateObservers.isEmpty()) {
                return;
            }
            NetworkStateUpdate stateUpdate = new NetworkStateUpdate(value);
            bisq.bi2p.protobuf.NetworkStateUpdate proto = stateUpdate.completeProto();
            networkStateObservers.forEach(observer -> {
                try {
                    observer.onNext(proto);
                } catch (Exception e) {
                    log.error("Error at notifying grpc observer {}", observer, e);
                }
            });
        });
        pins.add(pin);
    }

    private void observeRouterState() {
        var pin = routerMonitor.getRouterState().addObserver(value -> {
            if (value == null || routerStateObservers.isEmpty()) {
                return;
            }
            RouterStateUpdate stateUpdate = new RouterStateUpdate(value);
            bisq.bi2p.protobuf.RouterStateUpdate proto = stateUpdate.completeProto();
            routerStateObservers.forEach(observer -> {
                try {
                    observer.onNext(proto);
                } catch (Exception e) {
                    log.error("Error at notifying grpc observer {}", observer, e);
                }
            });
        });
        pins.add(pin);
    }

    private void observeTunnelInfo() {
        var pin = routerMonitor.getTunnelInfo().addObserver(value -> {
            if (value == null || tunnelInfoObservers.isEmpty()) {
                return;
            }
            TunnelInfoUpdate stateUpdate = new TunnelInfoUpdate(value);
            bisq.bi2p.protobuf.TunnelInfoUpdate proto = stateUpdate.completeProto();
            tunnelInfoObservers.forEach(observer -> {
                try {
                    observer.onNext(proto);
                } catch (Exception e) {
                    log.error("Error at notifying grpc observer {}", observer, e);
                }
            });
        });
        pins.add(pin);
    }
}
