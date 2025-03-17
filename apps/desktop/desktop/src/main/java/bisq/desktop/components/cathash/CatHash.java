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

import bisq.user.cathash.BucketConfig;
import bisq.user.profile.UserProfile;
import javafx.scene.image.Image;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

// Derived from https://github.com/neuhalje/android-robohash
@Slf4j
public class CatHash {
    @Setter
    private static JavaFxCatHashService delegate;

    public static Image getImage(UserProfile userProfile, double size) {
        return delegate.getImage(userProfile, size);
    }

    public static Image getImage(byte[] pubKeyHash, byte[] powSolution, int avatarVersion, double size) {
        return delegate.getImage(pubKeyHash, powSolution, avatarVersion, size);
    }

    // Remove the user profile icons which are not contained anymore in the current user profile list
    public static void pruneOutdatedProfileIcons(Collection<UserProfile> userProfiles) {
        delegate.pruneOutdatedProfileIcons(userProfiles);
    }

    public static int currentAvatarsVersion() {
        return BucketConfig.CURRENT_VERSION;
    }
}
