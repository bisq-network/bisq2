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
import bisq.bridge.protobuf.BsqBlockSubscriptionEvent;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BondedReputationDto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BsqBlockDto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.ProofOfBurnDto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.TxDto;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BsqBlocksRequest;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BsqBlocksResponse;
import bisq.user.reputation.data.AuthorizedBondedReputationData;
import bisq.user.reputation.data.AuthorizedProofOfBurnData;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * Requests BSQ blocks to extract AuthorizedProofOfBurnData and AuthorizedBondedReputationData.
 * Put the data into linkedBlockingQueues for processing by Bisq1BridgeService.
 */
@Slf4j
public class BsqBlockGrpcService extends BridgeSubscriptionGrpcService<BsqBlockDto> {
    @Getter
    private final BlockingQueue<AuthorizedProofOfBurnData> authorizedProofOfBurnDataQueue = new LinkedBlockingQueue<>(10000);
    @Getter
    private final BlockingQueue<AuthorizedBondedReputationData> authorizedBondedReputationDataQueue = new LinkedBlockingQueue<>(10000);
    private final IntConsumer liveBlockHandler;
    private final Set<String> processedTransactionIds = ConcurrentHashMap.newKeySet();
    private final AtomicInteger lastContiguousBlockHeight = new AtomicInteger();
    private volatile int latestSnapshotHeight;

    public BsqBlockGrpcService(boolean staticPublicKeysProvided,
                               GrpcClient grpcClient,
                               IntConsumer liveBlockHandler) {
        super(staticPublicKeysProvided, grpcClient);
        this.liveBlockHandler = liveBlockHandler;
        lastContiguousBlockHeight.set(super.getStartBlockHeight() - 1);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        authorizedProofOfBurnDataQueue.clear();
        authorizedBondedReputationDataQueue.clear();
        return super.shutdown();
    }

    @Override
    protected List<BsqBlockDto> doRequest(int startBlockHeight) {
        var protoRequest = new BsqBlocksRequest(startBlockHeight).completeProto();
        var protoResponse = GrpcClient.withBulkRequestDeadline(grpcClient.getBsqBlockBlockingStub())
                .requestBsqBlocks(protoRequest);
        BsqBlocksResponse response = BsqBlocksResponse.fromProto(protoResponse);
        response.verify();
        latestSnapshotHeight = response.getSnapshotHeight();
        return response.getBlocks();
    }

    @Override
    protected void handleResponse(BsqBlockDto data) {
        log.info("Received BsqBlockDto at height {}", data.getHeight());
        data.getTxDtoList()
                .stream()
                .filter(txDto -> processedTransactionIds.add(txDto.getTxId()))
                .forEach(txDto -> {
                    txDto.getProofOfBurnDto()
                            .map(proofOfBurnDto -> toAuthorizedProofOfBurnData(data, txDto, proofOfBurnDto))
                            .ifPresent(authorizedProofOfBurnDataQueue::offer);
                    txDto.getBondedReputationDto()
                            .map(bondedReputationDto -> toAuthorizedBondedReputationData(data, txDto, bondedReputationDto))
                            .ifPresent(authorizedBondedReputationDataQueue::offer);
                });
    }

    @Override
    protected int getStartBlockHeight() {
        return Math.max(super.getStartBlockHeight(), lastContiguousBlockHeight.get() + 1);
    }

    @Override
    protected void onHistoricalRequestComplete() {
        if (latestSnapshotHeight > 0) {
            lastContiguousBlockHeight.accumulateAndGet(latestSnapshotHeight, Math::max);
            liveBlockHandler.accept(latestSnapshotHeight);
        }
    }

    @Override
    protected void subscribe() {
        var subscription = BsqBlockSubscription.newBuilder().build();
        grpcClient.getBsqBlockStub().subscribeWithSnapshot(subscription, new StreamObserver<>() {
            @Override
            public void onNext(BsqBlockSubscriptionEvent event) {
                handleSubscriptionEvent(event);

                // reset
                subscribeRetryInterval.set(1);
            }

            @Override
            public void onError(Throwable throwable) {
                handleStreamObserverError(throwable);
            }

            @Override
            public void onCompleted() {
                handleStreamObserverCompleted();
            }
        });
    }

    void handleSubscriptionEvent(BsqBlockSubscriptionEvent event) {
        switch (event.getPayloadCase()) {
            case SUBSCRIPTIONREADYHEIGHT -> {
                log.info("BSQ block subscription established at height {}", event.getSubscriptionReadyHeight());
                requestAsync();
            }
            case BSQBLOCK -> handleLiveResponse(BsqBlockDto.fromProto(event.getBsqBlock()));
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException("BSQ block subscription event has no payload");
        }
    }

    void handleLiveResponse(BsqBlockDto block) {
        handleResponse(block);
        liveBlockHandler.accept(block.getHeight());
        advanceOrRecoverContiguousHeight(block.getHeight());
    }

    private void advanceOrRecoverContiguousHeight(int blockHeight) {
        while (true) {
            int lastHeight = lastContiguousBlockHeight.get();
            if (blockHeight <= lastHeight) {
                return;
            }
            if (blockHeight > lastHeight + 1) {
                requestAsync();
                return;
            }
            if (lastContiguousBlockHeight.compareAndSet(lastHeight, blockHeight)) {
                return;
            }
        }
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
                bondedReputationDto.getLockupTxId(),
                bondedReputationDto.getUnlockTxId(),
                staticPublicKeysProvided);
    }
}
