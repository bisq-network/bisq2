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
    private static final int BG1_COUNT = 31;
    private static final int EARS0_COUNT = 15;
    private static final int EARS1_COUNT = 2;
    private static final int FACE0_COUNT = 15;
    private static final int FACE1_COUNT = 15;
    private static final int EYES0_COUNT = 15;
    private static final int NOSE0_COUNT = 5;
    private static final int WHISKERS0_COUNT = 7;

    private final static int BUCKET_COUNT = 9;
    private final static int FACET_COUNT = 9;

    private final static byte[] BUCKET_SIZES = new byte[]{BG0_COUNT, BG1_COUNT, EARS0_COUNT, EARS1_COUNT, FACE0_COUNT,
            FACE1_COUNT, EYES0_COUNT, NOSE0_COUNT, WHISKERS0_COUNT};

    private final static String[] FACET_PATH_TEMPLATES;

    static {
        String postFix = ".png";
        FACET_PATH_TEMPLATES = new String[]{
                ROOT + "bg0/bg0_#ITEM#" + postFix,
                ROOT + "bg1/bg1_#ITEM#" + postFix,
                ROOT + "ears0/ears0_#ITEM#" + postFix,
                ROOT + "ears1/ears1_#ITEM#" + postFix,
                ROOT + "face0/face0_#ITEM#" + postFix,
                ROOT + "face1/face1_#ITEM#" + postFix,
                ROOT + "eyes0/eyes0_#ITEM#" + postFix,
                ROOT + "nose0/nose0_#ITEM#" + postFix,
                ROOT + "whiskers0/whiskers0_#ITEM#" + postFix,
        };
    }

    public String[] convertToFacetParts(byte[] bucketValues) {
        if (bucketValues.length != BUCKET_COUNT) {
            throw new IllegalArgumentException();
        }

        String[] paths = new String[FACET_COUNT];
        for (int facet = 0; facet < FACET_COUNT; facet++) {
            int bucketValue = bucketValues[facet];
            paths[facet] = generatePath(FACET_PATH_TEMPLATES[facet], bucketValue);
        }
        return paths;
    }

    private String generatePath(String facetPathTemplate, int bucketValue) {
        return facetPathTemplate.replaceAll("#ITEM#", String.format("%03d", bucketValue));
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
