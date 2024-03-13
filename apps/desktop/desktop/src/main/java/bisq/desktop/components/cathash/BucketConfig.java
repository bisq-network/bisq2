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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class BucketConfig {
    static final String DIGIT = "#";
    static final String SHAPE_NUMBER = "#SHAPE_NUMBER#";

    private static final Bucket BG = new Bucket(16, 0);
    private static final Bucket BG_OVERLAY = new Bucket(32, 1);
    private static final Bucket BODY_AND_FACE = new Bucket(16, 2);
    private static final Bucket CHEST_AND_EARS = new Bucket(16, 3);
    private static final Bucket CHEST_OVERLAY = new Bucket(3, 4);
    private static final Bucket EARS_OVERLAY = new Bucket(3, 5);
    private static final Bucket FACE_OVERLAY = new Bucket(17, 6);
    private static final Bucket EYES = new Bucket(16, 7);
    private static final Bucket NOSE = new Bucket(6, 8);
    private static final Bucket WHISKERS = new Bucket(7, 9);

    // Shape picker
    private static final Bucket BODY_SHAPE = new Bucket(2, 10);
    private static final Bucket CHEST_SHAPE = new Bucket(2, 11);
    private static final Bucket EARS_SHAPE = new Bucket(2, 12);
    private static final Bucket FACE_SHAPE = new Bucket(5, 13);

    private static final int[] BUCKET_SIZES = new int[]{
            BG.getCount(),
            BG_OVERLAY.getCount(),
            BODY_AND_FACE.getCount(),
            CHEST_AND_EARS.getCount(),
            CHEST_OVERLAY.getCount(),
            EARS_OVERLAY.getCount(),
            FACE_OVERLAY.getCount(),
            EYES.getCount(),
            NOSE.getCount(),
            WHISKERS.getCount(),
            BODY_SHAPE.getCount(),
            CHEST_SHAPE.getCount(),
            EARS_SHAPE.getCount(),
            FACE_SHAPE.getCount()
    };

    private static final Map<String, PathTemplateEncoding> PATH_TEMPLATES_WITH_ENCODING;

    static {
        String postFix = ".png";
        PATH_TEMPLATES_WITH_ENCODING = new LinkedHashMap<>();

        PATH_TEMPLATES_WITH_ENCODING.put("bg/bg_0/" + DIGIT + postFix, new PathTemplateEncoding(BG.getIdx()));
        PATH_TEMPLATES_WITH_ENCODING.put("bg/bg_1/" + DIGIT + postFix, new PathTemplateEncoding(BG_OVERLAY.getIdx()));
        PATH_TEMPLATES_WITH_ENCODING.put("body/body" + SHAPE_NUMBER + "/" + DIGIT + postFix,
                new PathTemplateEncoding(BODY_AND_FACE.getIdx(), BODY_SHAPE.getIdx()));
        PATH_TEMPLATES_WITH_ENCODING.put("chest/chest" + SHAPE_NUMBER + "_0/" + DIGIT + postFix,
                new PathTemplateEncoding(CHEST_AND_EARS.getIdx(), CHEST_SHAPE.getIdx()));
        PATH_TEMPLATES_WITH_ENCODING.put("chest/chest" + SHAPE_NUMBER + "_1/" + DIGIT + postFix,
                new PathTemplateEncoding(CHEST_OVERLAY.getIdx(), CHEST_SHAPE.getIdx()));
        PATH_TEMPLATES_WITH_ENCODING.put("ears/ears" + SHAPE_NUMBER + "_0/" + DIGIT + postFix,
                new PathTemplateEncoding(CHEST_AND_EARS.getIdx(), EARS_SHAPE.getIdx()));
        PATH_TEMPLATES_WITH_ENCODING.put("ears/ears" + SHAPE_NUMBER + "_1/" + DIGIT + postFix,
                new PathTemplateEncoding(EARS_OVERLAY.getIdx(), EARS_SHAPE.getIdx()));
        PATH_TEMPLATES_WITH_ENCODING.put("face/face" + SHAPE_NUMBER + "_0/" + DIGIT + postFix,
                new PathTemplateEncoding(BODY_AND_FACE.getIdx(), FACE_SHAPE.getIdx()));
        PATH_TEMPLATES_WITH_ENCODING.put("face/face" + SHAPE_NUMBER + "_1/" + DIGIT + postFix,
                new PathTemplateEncoding(FACE_OVERLAY.getIdx(), FACE_SHAPE.getIdx()));
        PATH_TEMPLATES_WITH_ENCODING.put("eyes/" + DIGIT + postFix, new PathTemplateEncoding(EYES.getIdx()));
        PATH_TEMPLATES_WITH_ENCODING.put("nose/" + DIGIT + postFix, new PathTemplateEncoding(NOSE.getIdx()));
        PATH_TEMPLATES_WITH_ENCODING.put("whiskers/" + DIGIT + postFix, new PathTemplateEncoding(WHISKERS.getIdx()));

        long numCombinations = getNumCombinations();
        log.info("Number of combinations: 2^{} = {}", MathUtils.getLog2(numCombinations), numCombinations);
    }

    static int[] getBucketSizes() {
        return BUCKET_SIZES;
    }

    static Map<String, PathTemplateEncoding> getPathTemplatesWithEncoding() {
        return PATH_TEMPLATES_WITH_ENCODING;
    }

    static long getNumCombinations() {
        long result = 1;
        for (int bucketSize : BUCKET_SIZES) {
            result *= bucketSize;
        }
        return result;
    }

    @Getter
    static class Bucket {
        int count;
        int idx;

        public Bucket(int count, int idx) {
            this.count = count;
            this.idx = idx;
        }
    }

    @Getter
    static class PathTemplateEncoding {
        Integer itemIdx;
        Integer shapeIdx;

        public PathTemplateEncoding(Integer itemIdx) {
            this(itemIdx, null);
        }

        public PathTemplateEncoding(Integer itemIdx, Integer shapeIdx) {
            this.itemIdx = itemIdx;
            this.shapeIdx = shapeIdx;
        }
    }
}
