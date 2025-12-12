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

package bisq.trade.mu_sig.grpc;

import bisq.common.application.Service;
import bisq.trade.protobuf.MusigGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class MusigGrpcClient implements Service {
    private final String host;
    private final int port;
    private ManagedChannel managedChannel;
    @Getter
    private MusigGrpc.MusigBlockingStub blockingStub;
    @Getter
    private MusigGrpc.MusigStub asyncStub;

    public MusigGrpcClient(String host, int port) {
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
                InsecureChannelCredentials.create()
        ).build();

        try {
            blockingStub = MusigGrpc.newBlockingStub(managedChannel);
            asyncStub = MusigGrpc.newStub(managedChannel);
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
            managedChannel = null;
        }
        blockingStub = null;
        asyncStub = null;
    }
}
