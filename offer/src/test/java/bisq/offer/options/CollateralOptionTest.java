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

package bisq.offer.options;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollateralOptionTest {
    @Test
    void validDepositsPassVerification() {
        assertThatCode(() -> new CollateralOption(0.15, 0.15)).doesNotThrowAnyException();
        assertThatCode(() -> new CollateralOption(0.0, 0.0)).doesNotThrowAnyException();
        assertThatCode(() -> new CollateralOption(1.0, 1.0)).doesNotThrowAnyException();
        // Asymmetry is a protocol-level restriction enforced by the take flow, not an
        // intrinsic invariant of the option.
        assertThatCode(() -> new CollateralOption(0.1, 0.2)).doesNotThrowAnyException();
    }

    @Test
    void nonFiniteDepositsAreRejected() {
        assertThatThrownBy(() -> new CollateralOption(Double.NaN, 0.15))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CollateralOption(0.15, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CollateralOption(Double.POSITIVE_INFINITY, 0.15))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CollateralOption(0.15, Double.NEGATIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void depositsOutsideZeroToOneAreRejected() {
        assertThatThrownBy(() -> new CollateralOption(-0.1, 0.15))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CollateralOption(0.15, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void signedZeroDepositsAreRejected() {
        // -0.0 is a non-canonical zero: it passes a plain >= 0 comparison but differs from
        // +0.0 under the Double.compare semantics the protocol uses downstream.
        assertThatThrownBy(() -> new CollateralOption(-0.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CollateralOption(0.0, -0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void protoRoundTripEnforcesVerification() {
        CollateralOption valid = new CollateralOption(0.25, 0.25);
        CollateralOption roundTripped = CollateralOption.fromProto(
                valid.toProto(true).getCollateralOption());
        assertThat(roundTripped).isEqualTo(valid);

        bisq.offer.protobuf.CollateralOption malformed = bisq.offer.protobuf.CollateralOption.newBuilder()
                .setBuyerSecurityDeposit(Double.NaN)
                .setSellerSecurityDeposit(0.25)
                .build();
        assertThatThrownBy(() -> CollateralOption.fromProto(malformed))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
