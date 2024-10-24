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

package bisq.user.cathash;

import java.math.BigInteger;
import java.util.Optional;

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

    static String[] toPaths(int[] buckets, BucketConfig.PathDetails[] pathTemplates) {
        String[] paths = new String[pathTemplates.length];
        for (int i = 0; i < paths.length; ++i) {
            String path = pathTemplates[i].getPath();
            Optional<Integer> shapeIdx = pathTemplates[i].getShapeIdx();
            int itemIdx = pathTemplates[i].getItemIdx();
            paths[i] = shapeIdx.map(idx -> generatePath(path, buckets[idx], buckets[itemIdx]))
                    .orElseGet(() -> generatePath(path, buckets[itemIdx]));
        }
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
