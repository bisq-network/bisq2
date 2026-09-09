/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.trade.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TradeProtocolFailureTest {
    @Test
    void setupAlreadyPendingRoundTripsThroughProtoAsAnExpectedFailure() {
        bisq.trade.protobuf.TradeProtocolFailure proto = TradeProtocolFailure.SETUP_ALREADY_PENDING.toProtoEnum();

        assertThat(proto).isEqualTo(bisq.trade.protobuf.TradeProtocolFailure.TRADEPROTOCOLFAILURE_SETUP_ALREADY_PENDING);
        assertThat(TradeProtocolFailure.fromProto(proto)).isEqualTo(TradeProtocolFailure.SETUP_ALREADY_PENDING);
        assertThat(TradeProtocolFailure.SETUP_ALREADY_PENDING.isUnexpected()).isFalse();
    }

    @Test
    void valueUnknownToThisClientFallsBackToUnknown() {
        // An older client receiving an appended value sees UNRECOGNIZED and must degrade to
        // UNKNOWN instead of failing to decode the report.
        assertThat(TradeProtocolFailure.fromProto(bisq.trade.protobuf.TradeProtocolFailure.UNRECOGNIZED))
                .isEqualTo(TradeProtocolFailure.UNKNOWN);
    }
}
