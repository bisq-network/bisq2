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

import io.grpc.stub.StreamObserver;

import lombok.extern.slf4j.Slf4j;



import bisq.oracle_node.bisq1_bridge.grpc.dto.BsqBlocks;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BurningmanBlocks;
import bisq.bridge.protobuf.BsqBlockSubscription;
import bisq.bridge.protobuf.BurningmanBlockSubscription;

@Slf4j
public class BridgeClient {
    private final GrpcClient grpcClient;

    public BridgeClient(GrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    public void initialize() {
        grpcClient.initialize();

        var subscription = BsqBlockSubscription.newBuilder().setStartBlockHeight(832353).build();
        grpcClient.getBsqBlockGrpcService().subscribe(subscription, new StreamObserver<>() {
            @Override
            public void onNext(bisq.bridge.protobuf.BsqBlocks proto) {
                BsqBlocks blocks = BsqBlocks.fromProto(proto);
                log.info("Received blocks: size={}\nblocks={}", blocks.getBlocks().size(), blocks);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error at BlockDtoSubscription", throwable);
            }

            @Override
            public void onCompleted() {
                log.info("BlockDtoSubscription completed");
            }
        });

        var burningManSubscription = BurningmanBlockSubscription.newBuilder().build();
        grpcClient.getBurningmanGrpcService().subscribe(burningManSubscription, new StreamObserver<>() {
            @Override
            public void onNext(bisq.bridge.protobuf.BurningmanBlocks proto) {
                BurningmanBlocks blocks = BurningmanBlocks.fromProto(proto);
                log.info("Received BurningManBlocks blocks: size={}\nblocks={}", blocks.getBlocks().size(), blocks);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error at BlockDtoSubscription", throwable);
            }

            @Override
            public void onCompleted() {
                log.info("BlockDtoSubscription completed");
            }
        });
    }
}
