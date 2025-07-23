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

import bisq.bridge.protobuf.BsqBlockSubscription;
import bisq.common.application.Service;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BondedReputationDto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BsqBlockDto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BsqBlocks;
import bisq.oracle_node.bisq1_bridge.grpc.dto.ProofOfBurnDto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.TxDto;
import bisq.user.reputation.data.AuthorizedBondedReputationData;
import bisq.user.reputation.data.AuthorizedProofOfBurnData;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BsqBlockGrpcService implements Service {
    private static final int LAUNCH_BLOCK_HEIGHT = 832353; // block height on Feb 28 2024

    private final boolean staticPublicKeysProvided;
    private final GrpcClient grpcClient;
    private final BlockingQueue<AuthorizedDistributedData> queue;
    private long retryInterval = 1;
    private boolean shutdownCalled;

    public BsqBlockGrpcService(boolean staticPublicKeysProvided,
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
        log.info("shutdown");
        shutdownCalled = true;
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void subscribe() {
        var subscription = BsqBlockSubscription.newBuilder()
                .setStartBlockHeight(LAUNCH_BLOCK_HEIGHT)
                .build();
        grpcClient.getBsqBlockGrpcService().subscribe(subscription, new StreamObserver<>() {

            @Override
            public void onNext(bisq.bridge.protobuf.BsqBlocks proto) {
                BsqBlocks blocks = BsqBlocks.fromProto(proto);
                log.info("Received {} blocks", blocks.getBlocks().size());
                blocks.getBlocks().forEach(bsqBlockDto -> {
                    bsqBlockDto.getTxDtoList()
                            .forEach(txDto -> {
                                txDto.getProofOfBurnDto()
                                        .map(proofOfBurnDto -> toAuthorizedProofOfBurnData(bsqBlockDto, txDto, proofOfBurnDto))
                                        .ifPresent(queue::offer);
                                txDto.getBondedReputationDto()
                                        .map(bondedReputationDto -> toAuthorizedBondedReputationData(bsqBlockDto, txDto, bondedReputationDto))
                                        .ifPresent(queue::offer);
                            });
                });
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error at BsqBlockSubscription {}", throwable.getMessage());
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
                log.info("BsqBlockSubscription completed");
            }
        });
    }

    private AuthorizedProofOfBurnData toAuthorizedProofOfBurnData(BsqBlockDto blockDto,
                                                                  TxDto txDto,
                                                                  ProofOfBurnDto proofOfBurnDto) {
        return new AuthorizedProofOfBurnData(
                blockDto.getTime(),
                proofOfBurnDto.getAmount(),
                proofOfBurnDto.getProofOfBurnHash(),
                blockDto.getHeight(),
                txDto.getTxId(),
                staticPublicKeysProvided);
    }

    private AuthorizedBondedReputationData toAuthorizedBondedReputationData(BsqBlockDto blockDto,
                                                                            TxDto txDto,
                                                                            BondedReputationDto bondedReputationDto) {
        return new AuthorizedBondedReputationData(
                blockDto.getTime(),
                bondedReputationDto.getAmount(),
                bondedReputationDto.getBondedReputationHash(),
                bondedReputationDto.getLockTime(),
                blockDto.getHeight(),
                txDto.getTxId(),
                staticPublicKeysProvided);
    }
}