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

import bisq.bi2p.protobuf.PeerAvailabilityRequest;
import bisq.bi2p.protobuf.RouterInfoRequest;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.network.i2p.grpc.messages.NetworkStateUpdate;
import bisq.network.i2p.grpc.messages.PeerAvailabilityResponse;
import bisq.network.i2p.grpc.messages.ProcessStateUpdate;
import bisq.network.i2p.grpc.messages.RouterInfoResponse;
import bisq.network.i2p.grpc.messages.RouterStateUpdate;
import bisq.network.i2p.grpc.messages.Topic;
import bisq.network.i2p.grpc.messages.TunnelInfoUpdate;
import bisq.network.i2p.router.state.NetworkState;
import bisq.network.i2p.router.state.ProcessState;
import bisq.network.i2p.router.state.RouterObserver;
import bisq.network.i2p.router.state.RouterState;
import bisq.network.i2p.router.state.TunnelInfo;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.i2p.data.Destination;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
public class Bi2pGrpcClientService implements RouterObserver, Service {
    private final Bi2pGrpcClient client;
    private final Observable<ProcessState> processState = new Observable<>(ProcessState.NEW);
    private final Observable<NetworkState> networkState = new Observable<>(NetworkState.NEW);
    private final Observable<RouterState> routerState = new Observable<>(RouterState.NEW);
    private final Observable<TunnelInfo> tunnelInfo = new Observable<>(new TunnelInfo());

    public Bi2pGrpcClientService(String host, int port) {
        log.info("Grpc client using {}:{}", host, port);
        client = new Bi2pGrpcClient(host, port);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return client.initialize();
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return client.shutdown();
    }

    public void requestRouterState() {
        RouterInfoRequest request = RouterInfoRequest.newBuilder().build();
        var proto = client.getBlockingStub().requestRouterInfo(request);
        RouterInfoResponse response = RouterInfoResponse.fromProto(proto);
        processState.set(response.getProcessState());
        networkState.set(response.getNetworkState());
        routerState.set(response.getRouterState());
        tunnelInfo.set(response.getTunnelInfo());
    }

    public PeerAvailabilityResponse requestPeerAvailability(Destination peersDestination) {
        PeerAvailabilityRequest request = PeerAvailabilityRequest.newBuilder()
                .setDestinationHash(ByteString.copyFrom(peersDestination.getHash().toByteArray()))
                .build();
        var proto = client.getBlockingStub().requestPeerAvailability(request);
        return PeerAvailabilityResponse.fromProto(proto);
    }

    public void subscribeAll() {
        Stream.of(Topic.values()).forEach(this::subscribe);
    }

    public void subscribe(Topic topic) {
        bisq.bi2p.protobuf.SubscribeRequest subscribeRequest = bisq.bi2p.protobuf.SubscribeRequest.newBuilder().setTopic(topic.toProtoEnum()).build();
        switch (topic) {
            case PROCESS_STATE -> {
                subscribeProcessState(subscribeRequest);
            }
            case NETWORK_STATE -> {
                subscribeNetworkState(subscribeRequest);
            }
            case ROUTER_STATE -> {
                subscribeRouterState(subscribeRequest);
            }
            case TUNNEL_INFO -> {
                subscribeTunnelInfo(subscribeRequest);
            }
            default -> throw new IllegalArgumentException("Topic not supported. topic=" + topic);
        }
    }

    @Override
    public ReadOnlyObservable<ProcessState> getProcessState() {
        return processState;
    }

    @Override
    public ReadOnlyObservable<NetworkState> getNetworkState() {
        return networkState;
    }

    @Override
    public ReadOnlyObservable<RouterState> getRouterState() {
        return routerState;
    }

    @Override
    public ReadOnlyObservable<TunnelInfo> getTunnelInfo() {
        return tunnelInfo;
    }


    private void subscribeProcessState(bisq.bi2p.protobuf.SubscribeRequest subscribeRequest) {
        client.getStub().subscribeProcessState(subscribeRequest, new StreamObserver<>() {
            @Override
            public void onNext(bisq.bi2p.protobuf.ProcessStateUpdate proto) {
                ProcessStateUpdate update = ProcessStateUpdate.fromProto(proto);
                log.info("ProcessState {} ", update.getValue());
                processState.set(update.getValue());
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error at subscribeProcessState", throwable);
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    private void subscribeNetworkState(bisq.bi2p.protobuf.SubscribeRequest subscribeRequest) {
        client.getStub().subscribeNetworkState(subscribeRequest, new StreamObserver<>() {
            @Override
            public void onNext(bisq.bi2p.protobuf.NetworkStateUpdate proto) {
                NetworkStateUpdate update = NetworkStateUpdate.fromProto(proto);
                log.info("NetworkState {} ", update.getValue());
                networkState.set(update.getValue());
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error at subscribeNetworkState", throwable);
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    private void subscribeRouterState(bisq.bi2p.protobuf.SubscribeRequest subscribeRequest) {
        client.getStub().subscribeRouterState(subscribeRequest, new StreamObserver<>() {
            @Override
            public void onNext(bisq.bi2p.protobuf.RouterStateUpdate proto) {
                RouterStateUpdate update = RouterStateUpdate.fromProto(proto);
                log.info("RouterState {} ", update.getValue());
                routerState.set(update.getValue());
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error at subscribeRouterState", throwable);
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    private void subscribeTunnelInfo(bisq.bi2p.protobuf.SubscribeRequest subscribeRequest) {
        client.getStub().subscribeTunnelInfo(subscribeRequest, new StreamObserver<>() {
            @Override
            public void onNext(bisq.bi2p.protobuf.TunnelInfoUpdate proto) {
                TunnelInfoUpdate update = TunnelInfoUpdate.fromProto(proto);
                log.info("TunnelInfo {} ", update.getValue());
                tunnelInfo.set(update.getValue());
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error at subscribeTunnelInfo", throwable);
            }

            @Override
            public void onCompleted() {
            }
        });
    }
}
