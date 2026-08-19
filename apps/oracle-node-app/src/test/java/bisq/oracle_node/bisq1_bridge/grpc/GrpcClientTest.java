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

package bisq.oracle_node.bisq1_bridge.grpc;

import bisq.bridge.protobuf.AccountAgeWitnessGrpcServiceGrpc;
import bisq.bridge.protobuf.BsqBlockGrpcServiceGrpc;
import io.grpc.Channel;
import io.grpc.Deadline;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GrpcClientTest {
    @Test
    void interactiveRequestsReceiveAFreshBoundedDeadline() {
        var stub = AccountAgeWitnessGrpcServiceGrpc.newBlockingStub(mock(Channel.class));

        Deadline deadline = GrpcClient.withInteractiveRequestDeadline(stub).getCallOptions().getDeadline();

        assertThat(deadline).isNotNull();
        assertThat(deadline.timeRemaining(TimeUnit.SECONDS))
                .isPositive()
                .isLessThanOrEqualTo(GrpcClient.INTERACTIVE_REQUEST_TIMEOUT_SECONDS);
    }

    @Test
    void bulkRequestsReceiveAFreshBoundedDeadline() {
        var stub = BsqBlockGrpcServiceGrpc.newBlockingStub(mock(Channel.class));

        Deadline deadline = GrpcClient.withBulkRequestDeadline(stub).getCallOptions().getDeadline();

        assertThat(deadline).isNotNull();
        assertThat(deadline.timeRemaining(TimeUnit.MINUTES))
                .isPositive()
                .isLessThanOrEqualTo(GrpcClient.BULK_REQUEST_TIMEOUT_MINUTES);
    }
}
