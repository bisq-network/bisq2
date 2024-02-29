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

import bisq.common.util.MathUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BucketConfig {
    static final String DIGIT = "#";

    private static final int BG0_COUNT = 16;
    private static final int BG1_COUNT = 16;
    private static final int EARS0_COUNT = 16;
    private static final int EARS1_COUNT = 3;
    private static final int FACE0_COUNT = 16;
    private static final int FACE1_COUNT = 9;
    private static final int EYES0_COUNT = 16;
    private static final int NOSE0_COUNT = 6;
    private static final int WHISKERS0_COUNT = 7;

    private static final int[] BUCKET_SIZES = new int[]{BG0_COUNT, BG1_COUNT, EARS0_COUNT, EARS1_COUNT, FACE0_COUNT,
            FACE1_COUNT, EYES0_COUNT, NOSE0_COUNT, WHISKERS0_COUNT};

    private static final String[] PATH_TEMPLATES;

    static {
        String postFix = ".png";
        PATH_TEMPLATES = new String[]{
                "bg0/" + DIGIT + postFix,
                "bg1/" + DIGIT + postFix,
                "ears0/" + DIGIT + postFix,
                "ears1/" + DIGIT + postFix,
                "face0/" + DIGIT + postFix,
                "face1/" + DIGIT + postFix,
                "eyes0/" + DIGIT + postFix,
                "nose0/" + DIGIT + postFix,
                "whiskers0/" + DIGIT + postFix
        };

        long numCombinations = getNumCombinations();
        log.info("Number of combinations: 2^{} = {}", MathUtils.getLog2(numCombinations), numCombinations);
    }

    static int[] getBucketSizes() {
        return BUCKET_SIZES;
    }

    static String[] getPathTemplates() {
        return PATH_TEMPLATES;
    }

    static long getNumCombinations() {
        long result = 1;
        for (int bucketSize : BUCKET_SIZES) {
            result *= bucketSize;
        }
        return result;
    }
}
