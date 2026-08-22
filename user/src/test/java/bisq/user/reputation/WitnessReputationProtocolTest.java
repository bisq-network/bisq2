/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.user.reputation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WitnessReputationProtocolTest {
    @Test
    void scoresFromTheLatestPossibleDateInTheBucket() {
        long dateBucket = TimeUnit.DAYS.toMillis(18_000);
        long latestPossibleDate = dateBucket + WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS - 1;

        assertThat(WitnessReputationProtocol.getConservativeAgeInDays(
                dateBucket,
                latestPossibleDate)).isZero();
        assertThat(WitnessReputationProtocol.getConservativeAgeInDays(
                dateBucket,
                latestPossibleDate + TimeUnit.DAYS.toMillis(61))).isEqualTo(61);
    }

    @ParameterizedTest
    @MethodSource("exactDateOffsets")
    void conservativeAgeNeverExceedsExactAge(long exactDateOffset) {
        long dateBucket = TimeUnit.DAYS.toMillis(18_000);
        assertThat(exactDateOffset).isBetween(0L,
                WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS - 1);
        long exactDate = dateBucket + exactDateOffset;
        long now = dateBucket + WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS +
                TimeUnit.DAYS.toMillis(100);
        long latestPossibleDate = WitnessReputationProtocol.getLatestPossibleDate(dateBucket);
        long exactAge = TimeUnit.MILLISECONDS.toDays(now - exactDate);
        long conservativeAge = WitnessReputationProtocol.getConservativeAgeInDays(dateBucket, now);

        assertThat(latestPossibleDate - exactDate)
                .isLessThan(WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS);
        assertThat(conservativeAge).isLessThanOrEqualTo(exactAge);
        assertThat(exactAge - conservativeAge).isLessThanOrEqualTo(1);
    }

    private static Stream<Long> exactDateOffsets() {
        return Stream.of(0L,
                WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS / 2,
                WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS - 1);
    }

    @Test
    void validatesOnlyOneDayBucketsAndThirtyTwoByteNullifiers() {
        long validBucket = Math.floorDiv(
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(100),
                WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS) *
                WitnessReputationProtocol.DATE_BUCKET_SIZE_MILLIS;

        WitnessReputationProtocol.validateDateBucket(validBucket);
        WitnessReputationProtocol.validateNullifier(
                new byte[WitnessReputationProtocol.NULLIFIER_LENGTH]);

        assertThatThrownBy(() -> WitnessReputationProtocol.validateDateBucket(validBucket + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WitnessReputationProtocol.validateNullifier(new byte[20]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
