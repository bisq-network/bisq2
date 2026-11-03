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

package bisq.api.offerbook;

import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;

import java.util.Optional;

// Whether the API shows an offer, and with which seller reputation verdict. Valid offers are shown like in the desktop
// offerbook. Own offers are shown also when they are invalid, so that the app can warn its user.
public record OfferVisibility(boolean shown, boolean valid, boolean hasSellerSufficientReputation) {
    private static final OfferVisibility HIDDEN = new OfferVisibility(false, false, false);
    // isValid already checks the reputation of a sell offer
    private static final OfferVisibility VALID = new OfferVisibility(true, true, true);

    // Empty when the visibility is unknown, while the maker profile is missing. A failing check propagates to the
    // caller, which knows whether it retries and how often to warn.
    public static Optional<OfferVisibility> find(BisqEasyOfferbookMessageService messageService,
                                                 UserProfileService userProfileService,
                                                 UserIdentityService userIdentityService,
                                                 BisqEasyOfferbookMessage message) {
        if (userProfileService.findUserProfile(message.getAuthorUserProfileId()).isEmpty()) {
            // The item shows the profile, so no item can be built without it. Checked without the profile, the
            // reputation service would use a score of 0 and cache the offer as insufficient. The ban and ignore
            // checks need no profile.
            boolean isBannedOrIgnored = !messageService.isNotBanned(message) || !messageService.isNotIgnored(message);
            return isBannedOrIgnored && !message.isMyMessage(userIdentityService)
                    ? Optional.of(HIDDEN)
                    : Optional.empty();
        }
        if (messageService.isValid(message)) {
            return Optional.of(VALID);
        }
        if (!message.isMyMessage(userIdentityService)) {
            return Optional.of(HIDDEN);
        }
        boolean verdict = messageService.hasSellerSufficientReputation(message);
        return Optional.of(new OfferVisibility(true, false, verdict));
    }
}
