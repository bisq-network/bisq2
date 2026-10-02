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

package bisq.network.p2p.node.network_load;

import bisq.common.proto.UnresolvableProtobufException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkLoadTest {
    @Test
    void valuesOutsideTheRangeAreLimited() {
        NetworkLoad networkLoad = new NetworkLoad(Double.POSITIVE_INFINITY, -1);

        assertEquals(1, networkLoad.getLoad());
        assertEquals(NetworkLoad.MIN_DIFFICULTY_ADJUSTMENT, networkLoad.getDifficultyAdjustmentFactor());
    }

    @Test
    void valueWhichIsNotANumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new NetworkLoad(Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new NetworkLoad(0.5, Double.NaN));
    }

    @Test
    void receivedValueWhichIsNotANumberFailsAtParsing() {
        bisq.network.protobuf.NetworkLoad proto = new NetworkLoad().toProto(false).toBuilder()
                .setLoad(Double.NaN)
                .build();

        assertThrows(UnresolvableProtobufException.class, () -> NetworkLoad.fromProto(proto));
    }
}
