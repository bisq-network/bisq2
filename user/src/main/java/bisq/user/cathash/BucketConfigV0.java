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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BucketConfigV0 extends BucketConfig {
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

    private static final PathDetails[] PATH_TEMPLATES;

    static {
        String postFix = ".png";
        PATH_TEMPLATES = new PathDetails[]{
                new PathDetails("bg/bg_0/" + DIGIT + postFix, BG.getIdx()),
                new PathDetails("bg/bg_1/" + DIGIT + postFix, BG_OVERLAY.getIdx()),
                new PathDetails("body/body" + SHAPE_NUMBER + "/" + DIGIT + postFix, BODY_AND_FACE.getIdx(), BODY_SHAPE.getIdx()),
                new PathDetails("chest/chest" + SHAPE_NUMBER + "_0/" + DIGIT + postFix, CHEST_AND_EARS.getIdx(), CHEST_SHAPE.getIdx()),
                new PathDetails("chest/chest" + SHAPE_NUMBER + "_1/" + DIGIT + postFix, CHEST_OVERLAY.getIdx(), CHEST_SHAPE.getIdx()),
                new PathDetails("ears/ears" + SHAPE_NUMBER + "_0/" + DIGIT + postFix, CHEST_AND_EARS.getIdx(), EARS_SHAPE.getIdx()),
                new PathDetails("ears/ears" + SHAPE_NUMBER + "_1/" + DIGIT + postFix, EARS_OVERLAY.getIdx(), EARS_SHAPE.getIdx()),
                new PathDetails("face/face" + SHAPE_NUMBER + "_0/" + DIGIT + postFix, BODY_AND_FACE.getIdx(), FACE_SHAPE.getIdx()),
                new PathDetails("face/face" + SHAPE_NUMBER + "_1/" + DIGIT + postFix, FACE_OVERLAY.getIdx(), FACE_SHAPE.getIdx()),
                new PathDetails("eyes/" + DIGIT + postFix, EYES.getIdx()),
                new PathDetails("nose/" + DIGIT + postFix, NOSE.getIdx()),
                new PathDetails("whiskers/" + DIGIT + postFix, WHISKERS.getIdx())
        };
    }

    @Override
    int[] getBucketSizes() {
        return BUCKET_SIZES;
    }

    @Override
    PathDetails[] getPathTemplates() {
        return PATH_TEMPLATES;
    }
}
