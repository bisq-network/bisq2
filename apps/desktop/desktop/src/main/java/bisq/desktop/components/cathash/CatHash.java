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

import bisq.common.data.ByteArray;
import bisq.desktop.common.utils.ImageUtil;
import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

// Derived from https://github.com/neuhalje/android-robohash
@Slf4j
public class CatHash {
    private static final int SIZE = 300;
    private static final int MAX_CACHE_SIZE = 10000;
    private static final ConcurrentHashMap<ByteArray, Image> CACHE = new ConcurrentHashMap<>();

    public static Image getImage(byte[] pubKeyHash) {
        return getImage(new ByteArray(pubKeyHash), true);
    }

    public static Image getImage(byte[] pubKeyHash, boolean useCache) {
        return getImage(new ByteArray(pubKeyHash), useCache);
    }

    private static Image getImage(ByteArray pubKeyHash, boolean useCache) {
        if (useCache && CACHE.containsKey(pubKeyHash)) {
            return CACHE.get(pubKeyHash);
        }
        BigInteger input = new BigInteger(pubKeyHash.getBytes());
        int[] buckets = BucketEncoder.encode(input, BucketConfig.getBucketSizes());
        String[] paths = BucketEncoder.toPaths(buckets, BucketConfig.getPathTemplates());
        Image image = ImageUtil.composeImage(paths, SIZE, SIZE);
        if (useCache && CACHE.size() < MAX_CACHE_SIZE) {
            CACHE.put(pubKeyHash, image);
        }
        return image;
    }
}
