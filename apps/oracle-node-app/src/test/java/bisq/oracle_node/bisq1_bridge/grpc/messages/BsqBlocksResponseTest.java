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

package bisq.oracle_node.bisq1_bridge.grpc.messages;

import bisq.oracle_node.bisq1_bridge.grpc.dto.BsqBlockDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BsqBlocksResponseTest {
    @Test
    void snapshotHeightRoundTripsEvenWhenTheSparseResponseHasNoBlocks() {
        BsqBlocksResponse response = new BsqBlocksResponse(List.of(), 941_123);

        BsqBlocksResponse roundTrip = BsqBlocksResponse.fromProto(response.toProto(false));

        assertThat(roundTrip).isEqualTo(response);
        assertThat(roundTrip.getBlocks()).isEmpty();
        assertThat(roundTrip.getSnapshotHeight()).isEqualTo(941_123);
    }

    @Test
    void responseRejectsBlocksAboveItsSnapshotHeight() {
        BsqBlocksResponse response = new BsqBlocksResponse(
                List.of(new BsqBlockDto(941_124, 0, List.of())),
                941_123);

        org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
                .isThrownBy(response::verify);
    }
}
