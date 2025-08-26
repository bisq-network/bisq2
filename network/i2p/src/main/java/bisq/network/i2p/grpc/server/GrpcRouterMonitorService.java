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

import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.i2p.protobuf.I2pRouterMonitorGrpc;
import bisq.network.i2p.grpc.messages.NetworkStateUpdate;
import bisq.network.i2p.grpc.messages.ProcessStateUpdate;
import bisq.network.i2p.grpc.messages.RouterStateUpdate;
import bisq.network.i2p.grpc.messages.TunnelInfoUpdate;
import bisq.network.i2p.router.I2pRouter;
import bisq.network.i2p.router.state.RouterMonitor;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class GrpcRouterMonitorService extends I2pRouterMonitorGrpc.I2pRouterMonitorImplBase implements Service {
    private final I2pRouter i2pRouter;

    private final Set<StreamObserver<bisq.i2p.protobuf.ProcessStateUpdate>> processStateObservers = new CopyOnWriteArraySet<>();
    private final Set<StreamObserver<bisq.i2p.protobuf.NetworkStateUpdate>> networkStateObservers = new CopyOnWriteArraySet<>();
    private final Set<StreamObserver<bisq.i2p.protobuf.RouterStateUpdate>> routerStateObservers = new CopyOnWriteArraySet<>();
    private final Set<StreamObserver<bisq.i2p.protobuf.TunnelInfoUpdate>> tunnelInfoObservers = new CopyOnWriteArraySet<>();
    private final Set<Pin> pins = new HashSet<>();
    private final RouterMonitor routerMonitor;

    public GrpcRouterMonitorService(I2pRouter i2pRouter) {
        this.i2pRouter = i2pRouter;
        routerMonitor = i2pRouter.getRouterMonitor();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        observeProcessState();
        observeNetworkState();
        observeRouterState();
        observeTunnelInfo();
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
    public void subscribeProcessState(bisq.i2p.protobuf.SubscribeRequest request,
                                      StreamObserver<bisq.i2p.protobuf.ProcessStateUpdate> responseObserver) {
        processStateObservers.add(responseObserver);
    }

    @Override
    public void subscribeNetworkState(bisq.i2p.protobuf.SubscribeRequest request,
                                      StreamObserver<bisq.i2p.protobuf.NetworkStateUpdate> responseObserver) {
        networkStateObservers.add(responseObserver);
    }

    @Override
    public void subscribeRouterState(bisq.i2p.protobuf.SubscribeRequest request,
                                     StreamObserver<bisq.i2p.protobuf.RouterStateUpdate> responseObserver) {
        routerStateObservers.add(responseObserver);
    }

    @Override
    public void subscribeTunnelInfo(bisq.i2p.protobuf.SubscribeRequest request,
                                    StreamObserver<bisq.i2p.protobuf.TunnelInfoUpdate> responseObserver) {
        tunnelInfoObservers.add(responseObserver);
    }

    private void observeProcessState() {
        var pin = routerMonitor.getProcessState().addObserver(networkState -> {
            if (networkState == null || networkStateObservers.isEmpty()) {
                return;
            }
            ProcessStateUpdate stateUpdate = new ProcessStateUpdate(networkState);
            processStateObservers.forEach(o -> o.onNext(stateUpdate.completeProto()));
        });
        pins.add(pin);
    }

    private void observeNetworkState() {
        var pin = routerMonitor.getNetworkState().addObserver(networkState -> {
            if (networkState == null || networkStateObservers.isEmpty()) {
                return;
            }
            NetworkStateUpdate stateUpdate = new NetworkStateUpdate(networkState);
            networkStateObservers.forEach(o -> o.onNext(stateUpdate.completeProto()));
        });
        pins.add(pin);
    }

    private void observeRouterState() {
        var pin = routerMonitor.getRouterState().addObserver(networkState -> {
            if (networkState == null || networkStateObservers.isEmpty()) {
                return;
            }
            RouterStateUpdate stateUpdate = new RouterStateUpdate(networkState);
            routerStateObservers.forEach(o -> o.onNext(stateUpdate.completeProto()));
        });
        pins.add(pin);
    }

    private void observeTunnelInfo() {
        var pin = routerMonitor.getTunnelInfo().addObserver(networkState -> {
            if (networkState == null || networkStateObservers.isEmpty()) {
                return;
            }
            TunnelInfoUpdate stateUpdate = new TunnelInfoUpdate(networkState);
            tunnelInfoObservers.forEach(o -> o.onNext(stateUpdate.completeProto()));
        });
        pins.add(pin);
    }
}
