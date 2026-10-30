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

package bisq.common.util;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkUtilsTest {
    @Test
    void aRandomPortIsAValidPort() {
        assertThat(NetworkUtils.selectRandomPort(new FixedRandom(0))).isEqualTo(1);
        assertThat(NetworkUtils.selectRandomPort(new FixedRandom(65_534))).isEqualTo(65_535);
    }

    private static final class FixedRandom extends Random {
        private final int value;

        private FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            assertThat(value).isLessThan(bound);
            return value;
        }
    }
}
