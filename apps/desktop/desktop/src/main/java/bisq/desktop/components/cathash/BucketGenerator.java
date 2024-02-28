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

package bisq.desktop.components.cathash;

import java.math.BigInteger;

public class BucketGenerator {
    static String[] integerBucketsToPaths(int[] integerBuckets,
                                          int bucketCount,
                                          int facetCount,
                                          String[] facetPathTemplates) {
        if (integerBuckets.length != bucketCount) {
            throw new IllegalArgumentException();
        }

        String[] paths = new String[facetCount];
        for (int facet = 0; facet < facetCount; facet++) {
            int bucketValue = integerBuckets[facet];
            paths[facet] = generatePath(facetPathTemplates[facet], bucketValue);
        }
        return paths;
    }

    /**
     * Takes the hash value and distributes it over the buckets.
     * <p>
     * Assumption: the value of hash is (much) larger than `16^bucketSizes.length` and uniformly distributed (random)
     *
     * @param input Any BigInteger that is to be split up in buckets according to the bucket configuration #bucketSizes.
     * @return buckets The distributed hash
     */
    static int[] createBuckets(BigInteger input, int[] bucketSizes) {
        int currentBucket = 0;
        int[] result = new int[bucketSizes.length];
        while (currentBucket < bucketSizes.length) {
            BigInteger[] divisorReminder = input.divideAndRemainder(BigInteger.valueOf(bucketSizes[currentBucket]));
            input = divisorReminder[0];
            long reminder = divisorReminder[1].longValue();
            result[currentBucket] = (int) Math.abs(reminder % bucketSizes[currentBucket]);
            currentBucket++;
        }
        return result;
    }

    private static String generatePath(String facetPathTemplate, int bucketValue) {
        return facetPathTemplate.replaceAll("NUM", String.format("%02d", bucketValue));
    }
}