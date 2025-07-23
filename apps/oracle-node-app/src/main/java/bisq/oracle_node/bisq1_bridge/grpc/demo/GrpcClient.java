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


import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;



import bisq.bridge.protobuf.BsqBlockGrpcServiceGrpc;
import bisq.bridge.protobuf.BurningmanGrpcServiceGrpc;

@Slf4j
public class GrpcClient {
    private final String host;
    private final int port;
    private ManagedChannel managedChannel;
    @Getter
    private BsqBlockGrpcServiceGrpc.BsqBlockGrpcServiceStub bsqBlockGrpcService;
    @Getter
    private BurningmanGrpcServiceGrpc.BurningmanGrpcServiceStub burningmanGrpcService;

    public GrpcClient(int port) {
        this("localhost", port);
    }

    public GrpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initialize() {
        managedChannel = Grpc.newChannelBuilderForAddress(
                host,
                port,
                InsecureChannelCredentials.create()
        ).build();

        try {
            bsqBlockGrpcService = BsqBlockGrpcServiceGrpc.newStub(managedChannel);
            burningmanGrpcService = BurningmanGrpcServiceGrpc.newStub(managedChannel);
        } catch (Exception e) {
            log.error("Initializing grpc client failed", e);
            dispose();
            throw e;
        }
    }

    public void shutDown() {
        log.info("shutdown");
        dispose();
    }

    private void dispose() {
        if (managedChannel != null) {
            managedChannel.shutdown();
            managedChannel = null;
        }
        bsqBlockGrpcService = null;
        burningmanGrpcService = null;
    }
}
