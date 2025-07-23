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

package bisq.oracle_node.bisq1_bridge.grpc.services;

import bisq.bridge.protobuf.BurningmanBlockSubscription;
import bisq.burningman.AuthorizedBurningmanListByBlock;
import bisq.burningman.BurningmanData;
import bisq.common.application.Service;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BurningmanBlockDto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BurningmanBlocks;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BurningmanDto;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BurningmanGrpcService implements Service {
    private static final int LAUNCH_BLOCK_HEIGHT = 832353; // block height on Feb 28 2024

    private final boolean staticPublicKeysProvided;
    private final GrpcClient grpcClient;
    private final BlockingQueue<AuthorizedDistributedData> queue;
    private long retryInterval = 1;
    private volatile boolean shutdownCalled;

    public BurningmanGrpcService(boolean staticPublicKeysProvided,
                                 GrpcClient grpcClient,
                                 BlockingQueue<AuthorizedDistributedData> queue) {
        this.staticPublicKeysProvided = staticPublicKeysProvided;
        this.grpcClient = grpcClient;
        this.queue = queue;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        subscribe();

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        shutdownCalled = true;
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void subscribe() {
        var subscription = BurningmanBlockSubscription.newBuilder().build();
        grpcClient.getBurningmanGrpcService().subscribe(subscription, new StreamObserver<>() {
            @Override
            public void onNext(bisq.bridge.protobuf.BurningmanBlocks proto) {
                BurningmanBlocks blocks = BurningmanBlocks.fromProto(proto);
                log.info("Received {} blocks", blocks.getBlocks().size());
                blocks.getBlocks().forEach(burningmanBlockDto -> {
                    List<BurningmanDto> burningManDtoList = burningmanBlockDto.getBurningmanDtoList();
                    if (!burningManDtoList.isEmpty()) {
                        var authorizedBurningmanListByBlock = toAuthorizedBurningmanListByBlock(burningmanBlockDto, burningManDtoList);
                        queue.offer(authorizedBurningmanListByBlock);
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error at BurningmanBlockSubscription {}", throwable.getMessage());
                // delayedExecutor is lightweight executor using ForkJoinPool.commonPool based on a global executor,
                // thus not expose an API for terminating it.
                log.error("We call subscribe again after {} ms", retryInterval);
                if (!shutdownCalled) {
                    CompletableFuture.delayedExecutor(retryInterval, TimeUnit.SECONDS).execute(() -> {
                        retryInterval = Math.min(10, ++retryInterval);
                        if (!shutdownCalled) {
                            subscribe();
                        }
                    });
                }
            }

            @Override
            public void onCompleted() {
                log.info("BlockDtoSubscription completed");
            }
        });
    }

    private AuthorizedBurningmanListByBlock toAuthorizedBurningmanListByBlock(BurningmanBlockDto burningmanBlockDto,
                                                                              List<BurningmanDto> burningManDtoList) {
        return new AuthorizedBurningmanListByBlock(
                staticPublicKeysProvided,
                burningmanBlockDto.getHeight(),
                burningManDtoList.stream()
                        .map(dto -> new BurningmanData(dto.getReceiverAddress(), dto.getCappedBurnAmountShare()))
                        .toList());
    }
}