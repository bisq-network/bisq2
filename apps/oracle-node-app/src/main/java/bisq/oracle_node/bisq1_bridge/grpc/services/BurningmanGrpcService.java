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
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BurningmanBlockDto;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BurningmanBlocksRequest;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BurningmanBlocksResponse;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class BurningmanGrpcService extends BridgeSubscriptionGrpcService<BurningmanBlockDto> {
    @Getter
    private final BlockingQueue<AuthorizedBurningmanListByBlock> authorizedBurningmanListByBlockQueue = new LinkedBlockingQueue<>(10000);

    public BurningmanGrpcService(boolean staticPublicKeysProvided, GrpcClient grpcClient) {
        super(staticPublicKeysProvided, grpcClient);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        authorizedBurningmanListByBlockQueue.clear();
        return super.shutdown();
    }

    @Override
    protected List<BurningmanBlockDto> doRequest(int startBlockHeight) {
        var protoRequest = new BurningmanBlocksRequest(startBlockHeight).completeProto();
        var protoResponse = grpcClient.getBurningmanBlockingStub().requestBurningmanBlocks(protoRequest);
        BurningmanBlocksResponse response = BurningmanBlocksResponse.fromProto(protoResponse);
        return response.getBlocks();
    }

    @Override
    protected void handleResponse(BurningmanBlockDto data) {
        log.info("Received BurningmanBlockDto at height {}", data.getHeight());
        var authorizedBurningmanListByBlock = toAuthorizedBurningmanListByBlock(data);
        authorizedBurningmanListByBlockQueue.offer(authorizedBurningmanListByBlock);
    }

    @Override
    protected void subscribe() {
        var subscription = BurningmanBlockSubscription.newBuilder().build();
        grpcClient.getBurningmanStub().subscribe(subscription, new StreamObserver<>() {
            @Override
            public void onNext(bisq.bridge.protobuf.BurningmanBlockDto proto) {
                handleResponse(BurningmanBlockDto.fromProto(proto));

                // reset
                subscribeRetryInterval.set(1);
            }

            @Override
            public void onError(Throwable throwable) {
                handleStreamObserverError(throwable);
            }

            @Override
            public void onCompleted() {
                log.info("BlockDtoSubscription completed");
            }
        });
    }

    private AuthorizedBurningmanListByBlock toAuthorizedBurningmanListByBlock(BurningmanBlockDto burningmanBlockDto) {
        return new AuthorizedBurningmanListByBlock(
                staticPublicKeysProvided,
                burningmanBlockDto.getHeight(),
                burningmanBlockDto.getItems().stream()
                        .map(dto -> new BurningmanData(dto.getReceiverAddress(), dto.getCappedBurnAmountShare()))
                        .toList());
    }
}