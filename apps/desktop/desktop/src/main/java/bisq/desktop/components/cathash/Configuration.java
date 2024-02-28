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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Configuration {
    private final static String ROOT = "";

    private static final int BG0_COUNT = 15;
    private static final int BG1_COUNT = 15;
    private static final int EARS0_COUNT = 15;
    private static final int EARS1_COUNT = 2;
    private static final int FACE0_COUNT = 15;
    private static final int FACE1_COUNT = 8;
    private static final int EYES0_COUNT = 15;
    private static final int NOSE0_COUNT = 5;
    private static final int WHISKERS0_COUNT = 6;

    private final static int BUCKET_COUNT = 9;
    private final static int FACET_COUNT = 9;

    private final static byte[] BUCKET_SIZES = new byte[]{BG0_COUNT, BG1_COUNT, EARS0_COUNT, EARS1_COUNT, FACE0_COUNT,
            FACE1_COUNT, EYES0_COUNT, NOSE0_COUNT, WHISKERS0_COUNT};

    private final static String[] FACET_PATH_TEMPLATES;

    static {
        String postFix = ".png";
        FACET_PATH_TEMPLATES = new String[]{
                ROOT + "bg0/NUM" + postFix,
                ROOT + "bg1/NUM" + postFix,
                ROOT + "ears0/NUM" + postFix,
                ROOT + "ears1/NUM" + postFix,
                ROOT + "face0/NUM" + postFix,
                ROOT + "face1/NUM" + postFix,
                ROOT + "eyes0/NUM" + postFix,
                ROOT + "nose0/NUM" + postFix,
                ROOT + "whiskers0/NUM" + postFix,
        };
    }

    public String[] integerBucketsToPaths(int[] integerBuckets) {
        if (integerBuckets.length != BUCKET_COUNT) {
            throw new IllegalArgumentException();
        }

        String[] paths = new String[FACET_COUNT];
        for (int facet = 0; facet < FACET_COUNT; facet++) {
            int bucketValue = integerBuckets[facet];
            paths[facet] = generatePath(FACET_PATH_TEMPLATES[facet], bucketValue);
        }
        return paths;
    }

    private String generatePath(String facetPathTemplate, int bucketValue) {
        return facetPathTemplate.replaceAll("NUM", String.format("%02d", bucketValue));
    }

    public byte[] getBucketSizes() {
        return BUCKET_SIZES;
    }

    public int width() {
        return 300;
    }

    public int height() {
        return 300;
    }
}
