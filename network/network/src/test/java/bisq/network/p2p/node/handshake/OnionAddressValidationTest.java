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

package bisq.network.p2p.node.handshake;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class OnionAddressValidationTest {
    private static final long NOW = 1_790_998_800_000L;

    @ParameterizedTest
    @CsvSource({"-7200001, false", "-7200000, true", "-1, true", "0, true", "1, true",
            "7200000, true", "7200001, false"})
    void acceptsOnlyDatesWithinTwoHours(long offset, boolean accepted) {
        assertThat(OnionAddressValidation.isDateWithinTolerance(NOW + offset, NOW)).isEqualTo(accepted);
    }

    @Test
    void rejectsADifferenceWhichOverflowsToLongMinValue() {
        assertThat(OnionAddressValidation.isDateWithinTolerance(Long.MIN_VALUE + NOW, NOW)).isFalse();
    }

    @Test
    void rejectsAnExactLongMinValueDifference() {
        assertThat(OnionAddressValidation.isDateWithinTolerance(0, Long.MIN_VALUE)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, Long.MAX_VALUE})
    void rejectsExtremePeerDates(long date) {
        assertThat(OnionAddressValidation.isDateWithinTolerance(date, NOW)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, Long.MAX_VALUE})
    void acceptsTheSameDateAtTheLongLimits(long now) {
        assertThat(OnionAddressValidation.isDateWithinTolerance(now, now)).isTrue();
    }
}
