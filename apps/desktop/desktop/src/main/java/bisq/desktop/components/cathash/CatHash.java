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

import bisq.common.util.ByteArrayUtils;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.user.profile.UserProfile;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

// Derived from https://github.com/neuhalje/android-robohash
@Slf4j
public class CatHash {
    private static final int SIZE = 300;
    private static final int MAX_CACHE_SIZE = 10000;
    private static final ConcurrentHashMap<BigInteger, Image> CACHE = new ConcurrentHashMap<>();

    public static Button getIconButton(UserProfile userProfile, double iconSize) {
        return BisqIconButton.createIconButton(getImageView(userProfile, iconSize));
    }

    public static ImageView getImageView(UserProfile userProfile, double iconSize) {
        ImageView imageView = new ImageView(getImage(userProfile));
        imageView.setFitWidth(iconSize);
        imageView.setFitHeight(iconSize);
        return imageView;
    }

    public static Image getImage(UserProfile userProfile) {
        return getImage(userProfile.getPubKeyHash(), userProfile.getProofOfWork().getSolution(),
                userProfile.getAvatarVersion());
    }

    public static Image getImage(byte[] pubKeyHash, byte[] powSolution, int avatarVersion) {
        return getImage(pubKeyHash, powSolution, avatarVersion, true);
    }

    public static Image getImage(byte[] pubKeyHash, byte[] powSolution, int avatarVersion, boolean useCache) {
        byte[] combined = ByteArrayUtils.concat(powSolution, pubKeyHash);
        BigInteger input = new BigInteger(combined);
        if (useCache && CACHE.containsKey(input)) {
            return CACHE.get(input);
        }

        BucketConfig bucketConfig = getBucketConfig(avatarVersion);
        log.debug("Getting user avatar image using class {}", bucketConfig.getClass().getName());

        int[] buckets = BucketEncoder.encode(input, bucketConfig.getBucketSizes());
        String[] paths = BucketEncoder.toPaths(buckets, bucketConfig.getPathTemplates());
        Image image = ImageUtil.composeImage(paths, SIZE, SIZE);
        if (useCache && CACHE.size() < MAX_CACHE_SIZE) {
            CACHE.put(input, image);
        }
        return image;
    }

    public static int currentAvatarsVersion() {
        return BucketConfig.CURRENT_VERSION;
    }

    private static BucketConfig getBucketConfig(int avatarVersion) {
        BucketConfig bucketConfig;
        if (avatarVersion == 0) {
            bucketConfig = new BucketConfigV0();
        } else {
            throw new IllegalArgumentException("Provided avatarVersion not supported. avatarVersion=" + avatarVersion);
        }
        return bucketConfig;
    }
}
