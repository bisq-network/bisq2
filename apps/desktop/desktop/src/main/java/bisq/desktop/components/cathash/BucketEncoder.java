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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BucketEncoder {
    /**
     * @param input A BigInteger input that is to be split up deterministically in buckets according to the bucketSizes array.
     * @return buckets
     */
    static int[] encode(BigInteger input, int[] bucketSizes) {
        int currentBucket = 0;
        int[] result = new int[bucketSizes.length];
        while (currentBucket < bucketSizes.length) {
            int bucketSize = bucketSizes[currentBucket];
            BigInteger[] divisorReminder = input.divideAndRemainder(BigInteger.valueOf(bucketSize));
            input = divisorReminder[0];
            long reminder = divisorReminder[1].longValue();
            result[currentBucket] = (int) Math.abs(reminder % bucketSize);
            currentBucket++;
        }
        return result;
    }

    static String[] toPaths(int[] buckets, Map<String, BucketConfig.PathTemplateEncoding> pathTemplatesWithEncoding) {
        String[] paths = new String[pathTemplatesWithEncoding.size()];
        AtomicInteger idx = new AtomicInteger(0);
        pathTemplatesWithEncoding.forEach((path, encoding) -> {
            Integer shapeNumber = encoding.shapeIdx != null ? buckets[encoding.shapeIdx] : null;
            int itemNumber = buckets[encoding.itemIdx];
            paths[idx.getAndIncrement()] = shapeNumber != null
                    ? generatePath(path, shapeNumber, itemNumber)
                    : generatePath(path, itemNumber);
        });
        return paths;
    }

    private static String generatePath(String pathTemplate, int shapeNumber, int index) {
        return pathTemplate
                .replaceAll(BucketConfig.SHAPE_NUMBER, String.format("%1d", shapeNumber))
                .replaceAll(BucketConfig.DIGIT, String.format("%02d", index));
    }

    private static String generatePath(String pathTemplate, int index) {
        return pathTemplate.replaceAll(BucketConfig.DIGIT, String.format("%02d", index));
    }
}
