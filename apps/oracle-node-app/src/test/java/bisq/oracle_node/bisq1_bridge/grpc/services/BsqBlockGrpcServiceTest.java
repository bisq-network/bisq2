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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.oracle_node.bisq1_bridge.grpc.services;

import bisq.bridge.protobuf.BsqBlockSubscriptionEvent;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.dto.BsqBlockDto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.ProofOfBurnDto;
import bisq.oracle_node.bisq1_bridge.grpc.dto.TxDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class BsqBlockGrpcServiceTest {
    @Test
    void anEmptyLiveBlockStillTriggersRegistrationRevalidation() {
        AtomicInteger notifiedHeight = new AtomicInteger(-1);
        BsqBlockGrpcService service = new BsqBlockGrpcService(false,
                mock(GrpcClient.class),
                notifiedHeight::set);
        int initialStartHeight = service.getStartBlockHeight();

        service.handleLiveResponse(new BsqBlockDto(initialStartHeight, 0, List.of()));

        assertThat(notifiedHeight).hasValue(initialStartHeight);
        assertThat(service.getAuthorizedProofOfBurnDataQueue()).isEmpty();
        assertThat(service.getAuthorizedBondedReputationDataQueue()).isEmpty();
    }

    @Test
    void contiguousLiveBlocksAdvanceTheCatchUpCursor() {
        BsqBlockGrpcService service = new BsqBlockGrpcService(false,
                mock(GrpcClient.class),
                ignored -> {
                });
        int initialStartHeight = service.getStartBlockHeight();

        service.handleLiveResponse(new BsqBlockDto(initialStartHeight, 0, List.of()));

        assertThat(service.getStartBlockHeight()).isEqualTo(initialStartHeight + 1);
    }

    @Test
    void liveGapRequestsCatchUpWithoutSkippingTheMissingHeight() {
        AtomicInteger notificationCount = new AtomicInteger();
        BsqBlockGrpcService service = spy(new BsqBlockGrpcService(false,
                mock(GrpcClient.class),
                ignored -> notificationCount.incrementAndGet()));
        doNothing().when(service).requestAsync();
        int initialStartHeight = service.getStartBlockHeight();
        TxDto tx = new TxDto("b".repeat(64),
                Optional.of(new ProofOfBurnDto(1, new byte[20])),
                Optional.empty());

        service.handleLiveResponse(new BsqBlockDto(initialStartHeight + 1, 0, List.of(tx)));

        assertThat(service.getStartBlockHeight()).isEqualTo(initialStartHeight);
        assertThat(notificationCount).hasValue(0);
        assertThat(service.getAuthorizedProofOfBurnDataQueue()).isEmpty();
        assertThat(service.getProcessedTransactionCount()).isZero();
        verify(service).requestAsync();
    }

    @Test
    void duplicateLiveBlockDoesNotTriggerRegistrationRevalidationAgain() {
        AtomicInteger notificationCount = new AtomicInteger();
        BsqBlockGrpcService service = new BsqBlockGrpcService(false,
                mock(GrpcClient.class),
                ignored -> notificationCount.incrementAndGet());
        int initialStartHeight = service.getStartBlockHeight();
        BsqBlockDto block = new BsqBlockDto(initialStartHeight, 0, List.of());

        service.handleLiveResponse(block);
        service.handleLiveResponse(block);

        assertThat(notificationCount).hasValue(1);
    }

    @Test
    void subscriptionReadyEventRequestsCatchUpWithoutAdvancingTheCursor() {
        BsqBlockGrpcService service = spy(new BsqBlockGrpcService(false,
                mock(GrpcClient.class),
                ignored -> {
                }));
        doNothing().when(service).requestAsync();
        int initialStartHeight = service.getStartBlockHeight();

        service.handleSubscriptionEvent(BsqBlockSubscriptionEvent.newBuilder()
                .setSubscriptionReadyHeight(initialStartHeight + 10)
                .build());

        assertThat(service.getStartBlockHeight()).isEqualTo(initialStartHeight);
        verify(service).requestAsync();
    }

    @Test
    void snapshotAndLiveOverlapDoesNotQueueATransactionTwice() {
        BsqBlockGrpcService service = new BsqBlockGrpcService(false,
                mock(GrpcClient.class),
                ignored -> {
                });
        String txId = "a".repeat(64);
        TxDto tx = new TxDto(txId,
                Optional.of(new ProofOfBurnDto(1, new byte[20])),
                Optional.empty());
        int historicalStartHeight = service.prepareHistoricalRequest();
        BsqBlockDto block = new BsqBlockDto(historicalStartHeight,
                System.currentTimeMillis(),
                List.of(tx));

        service.handleResponse(block);
        service.handleLiveResponse(block);

        assertThat(service.getAuthorizedProofOfBurnDataQueue()).hasSize(1);
        assertThat(service.getProcessedTransactionCount()).isEqualTo(1);

        service.onHistoricalRequestFinished(true);
        service.handleLiveResponse(block);

        assertThat(service.getAuthorizedProofOfBurnDataQueue()).hasSize(1);
        assertThat(service.getProcessedTransactionCount()).isZero();
    }

    @Test
    void failedHistoricalRequestReleasesItsDeduplicationEntries() {
        BsqBlockGrpcService service = new BsqBlockGrpcService(false,
                mock(GrpcClient.class),
                ignored -> {
                });
        TxDto tx = new TxDto("c".repeat(64),
                Optional.of(new ProofOfBurnDto(1, new byte[20])),
                Optional.empty());
        int historicalStartHeight = service.prepareHistoricalRequest();

        service.handleResponse(new BsqBlockDto(historicalStartHeight,
                System.currentTimeMillis(),
                List.of(tx)));
        assertThat(service.getProcessedTransactionCount()).isEqualTo(1);

        service.onHistoricalRequestFinished(false);

        assertThat(service.getProcessedTransactionCount()).isZero();
    }
}
