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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class BucketConfig {
    public static final int CURRENT_VERSION = 0;
    static final String DIGIT = "#";
    static final String SHAPE_NUMBER = "#SHAPE_NUMBER#";

    abstract int[] getBucketSizes();

    abstract PathDetails[] getPathTemplates();

    @Getter
    static class Bucket {
        final int count;
        final int idx;

        public Bucket(int count, int idx) {
            this.count = count;
            this.idx = idx;
        }
    }

    @Getter
    static class PathDetails {
        final String path;
        final int itemIdx;
        final Optional<Integer> shapeIdx;

        public PathDetails(String path, Integer itemIdx) {
            this(path, itemIdx, null);
        }

        public PathDetails(String path, Integer itemIdx, Integer shapeIdx) {
            this.path = path;
            this.itemIdx = itemIdx;
            this.shapeIdx = Optional.ofNullable(shapeIdx);
        }
    }
}
