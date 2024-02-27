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

package bisq.desktop.components.robohash;

import java.math.BigInteger;
import java.util.UUID;

/**
 * "Hash" a big integer (expected: hash or uuid) into buckets. The goal is to deterministically
 * "repack" the randomness of the hash into the bucket.
 * <p>
 * Each bucket is defined by a maximum value. The implementation guarantees that the values in bucket n is in the
 * range 0..(bucketSize[n]-1).
 */
public class VariableSizeHashing {
    private final byte[] bucketSizes;

    public VariableSizeHashing(byte[] bucketSizes) {
        this.bucketSizes = bucketSizes;
    }

    static BigInteger uuidToBigInteger(UUID uuid) {
        return BigInteger.valueOf(uuid.getMostSignificantBits()).shiftLeft(64).add(BigInteger.valueOf(uuid.getLeastSignificantBits()));
    }

    public byte[] createBuckets(UUID uuid) {
        return createBuckets(uuidToBigInteger(uuid));
    }

    /**
     * Takes the hash value and distributes it over the buckets.
     * <p>
     * Assumption: the value of hash is (much) larger than `16^bucketSizes.length` and uniformly distributed (random)
     *
     * @param hash Any BigInteger that is to be split up in buckets according to the bucket configuration #bucketSizes.
     * @return buckets The distributed hash
     */
    public byte[] createBuckets(BigInteger hash) {
        int currentBucket = 0;
        byte[] ret = new byte[this.bucketSizes.length];

        while (currentBucket < this.bucketSizes.length) {
            BigInteger[] divisorReminder = hash.divideAndRemainder(BigInteger.valueOf(bucketSizes[currentBucket]));

            hash = divisorReminder[0];
            long reminder = divisorReminder[1].longValue();

            ret[currentBucket] = (byte) Math.abs(reminder % bucketSizes[currentBucket]);

            currentBucket += 1;
        }

        return ret;
    }
}
