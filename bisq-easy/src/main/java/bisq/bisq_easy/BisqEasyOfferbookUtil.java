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

package bisq.bisq_easy;

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;

import java.util.Optional;

public class BisqEasyOfferbookUtil {

    public static boolean authorNotBannedOrIgnored(UserProfileService userProfileService,
                                                   BannedUserService bannedUserService,
                                                   BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
        String authorUserProfileId = bisqEasyOfferbookMessage.getAuthorUserProfileId();
        Optional<UserProfile> senderUserProfile = userProfileService.findUserProfile(authorUserProfileId);
        if (senderUserProfile.isEmpty() ||
                !bisqEasyOfferbookMessage.hasBisqEasyOffer()) {
            return false;
        }

        UserProfile userProfile = senderUserProfile.get();
        boolean isSenderBanned = bannedUserService.isUserProfileBanned(authorUserProfileId)
                || bannedUserService.isUserProfileBanned(userProfile);
        if (isSenderBanned) {
            return false;
        }

        if (userProfileService.getIgnoredUserProfileIds().contains(userProfile.getId())) {
            return false;
        }
        return true;
    }
}
