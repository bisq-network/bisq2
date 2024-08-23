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

import bisq.common.encoding.Hex;
import bisq.common.util.ByteArrayUtils;
import bisq.desktop.common.utils.ImageUtil;
import bisq.user.profile.UserProfile;
import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

// Derived from https://github.com/neuhalje/android-robohash
@Slf4j
public class CatHash {
    // Largest size in offerbook is 60, in reputationListView ist 40 and in chats 30.
    // Larger images are used only rarely and are nto cached.
    public static final double SIZE_OF_CACHED_ICONS = 60;

    // This is a 120*120 image meaning 14 400 pixels. At 4 bytes each, that takes 57.6 KB in memory.
    // With 5000 images we would get about 288 MB.
    private static final int MAX_CACHE_SIZE = 5000;
    private static final ConcurrentHashMap<BigInteger, Image> CACHE = new ConcurrentHashMap<>();

    public static Image getImage(UserProfile userProfile, double size) {
        return getImage(userProfile.getPubKeyHash(),
                userProfile.getProofOfWork().getSolution(),
                userProfile.getAvatarVersion(),
                size);
    }

    public static Image getImage(byte[] pubKeyHash, byte[] powSolution, int avatarVersion, double size) {
        byte[] combined = ByteArrayUtils.concat(powSolution, pubKeyHash);
        BigInteger input = new BigInteger(combined);
        boolean useCache = size <= SIZE_OF_CACHED_ICONS;
        if (useCache && CACHE.containsKey(input)) {
            return CACHE.get(input);
        }

        long ts = System.currentTimeMillis();
        BucketConfig bucketConfig = getBucketConfig(avatarVersion);
        int[] buckets = BucketEncoder.encode(input, bucketConfig.getBucketSizes());
        String[] paths = BucketEncoder.toPaths(buckets, bucketConfig.getPathTemplates());
        // For retina support we scale by 2
        Image image = ImageUtil.composeImage(paths, 2 * SIZE_OF_CACHED_ICONS);
        log.debug("Creating user profile icon for {} took {} ms.", Hex.encode(pubKeyHash), System.currentTimeMillis() - ts);
        if (useCache && CACHE.size() < MAX_CACHE_SIZE) {
            CACHE.put(input, image);
        }
        return image;
    }

    public static int currentAvatarsVersion() {
        return BucketConfig.CURRENT_VERSION;
    }

    private static BucketConfig getBucketConfig(int avatarVersion) {
        if (avatarVersion == 0) {
            return new BucketConfigV0();
        } else {
            throw new IllegalArgumentException("Provided avatarVersion not supported. avatarVersion=" + avatarVersion);
        }
    }
}
