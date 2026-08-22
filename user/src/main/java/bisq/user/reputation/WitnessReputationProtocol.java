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

import bisq.common.validation.NetworkDataValidation;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public final class WitnessReputationProtocol {
    public static final int NULLIFIER_LENGTH = 32;
    public static final long DATE_BUCKET_SIZE_MILLIS = TimeUnit.DAYS.toMillis(1);

    private static final long DAY_AS_MILLIS = TimeUnit.DAYS.toMillis(1);
    private static final long EARLIEST_DATE_BUCKET = Math.floorDiv(
            NetworkDataValidation.BISQ_1_LAUNCH_DATE,
            DATE_BUCKET_SIZE_MILLIS) * DATE_BUCKET_SIZE_MILLIS;

    private WitnessReputationProtocol() {
    }

    public static void validateDateBucket(long dateBucket) {
        checkArgument(dateBucket % DATE_BUCKET_SIZE_MILLIS == 0,
                "Witness reputation date is not a one-day UTC bucket");
        checkArgument(dateBucket >= EARLIEST_DATE_BUCKET &&
                        dateBucket < System.currentTimeMillis() + NetworkDataValidation.TWO_HOURS,
                "Witness reputation date bucket is out of range");
    }

    public static void validateNullifier(byte[] witnessNullifier) {
        checkArgument(witnessNullifier.length == NULLIFIER_LENGTH,
                "Witness reputation nullifier must be 32 bytes");
    }

    public static long getLatestPossibleDate(long dateBucket) {
        return Math.addExact(dateBucket, DATE_BUCKET_SIZE_MILLIS - 1);
    }

    public static long getConservativeAgeInDays(long dateBucket) {
        return getConservativeAgeInDays(dateBucket, System.currentTimeMillis());
    }

    static long getConservativeAgeInDays(long dateBucket, long now) {
        long latestPossibleDate = getLatestPossibleDate(dateBucket);
        return now <= latestPossibleDate ? 0 : (now - latestPossibleDate) / DAY_AS_MILLIS;
    }
}
