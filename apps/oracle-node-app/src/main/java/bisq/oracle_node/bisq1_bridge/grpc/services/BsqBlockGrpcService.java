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
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
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
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class BsqBlockGrpcService extends BridgeSubscriptionGrpcService<BsqBlockDto> {
    public BsqBlockGrpcService(boolean staticPublicKeysProvided,
                               GrpcClient grpcClient,
                               BlockingQueue<AuthorizedDistributedData> queue) {
        super(staticPublicKeysProvided, grpcClient, queue);
    }

    @Override
    protected List<BsqBlockDto> doRequest(int startBlockHeight) {
        var protoRequest = new BsqBlocksRequest(startBlockHeight).completeProto();
        var protoResponse = grpcClient.getBsqBlockBlockingStub().requestBsqBlocks(protoRequest);
        BsqBlocksResponse response = BsqBlocksResponse.fromProto(protoResponse);
        return response.getBlocks();
    }

    @Override
    protected void handleResponse(BsqBlockDto data) {
        log.info("Received BsqBlockDto at height {}", data.getHeight());
        data.getTxDtoList()
                .forEach(txDto -> {
                    txDto.getProofOfBurnDto()
                            .map(proofOfBurnDto -> toAuthorizedProofOfBurnData(data, txDto, proofOfBurnDto))
                            .ifPresent(queue::offer);
                    txDto.getBondedReputationDto()
                            .map(bondedReputationDto -> toAuthorizedBondedReputationData(data, txDto, bondedReputationDto))
                            .ifPresent(queue::offer);
                });
    }

    @Override
    protected void subscribe() {
        var subscription = BsqBlockSubscription.newBuilder().build();
        grpcClient.getBsqBlockStub().subscribe(subscription, new StreamObserver<>() {
            @Override
            public void onNext(bisq.bridge.protobuf.BsqBlockDto proto) {
                handleResponse(BsqBlockDto.fromProto(proto));

                // reset
                subscribeRetryInterval.set(1);
            }

            @Override
            public void onError(Throwable throwable) {
                handleStreamObserverError(throwable);
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