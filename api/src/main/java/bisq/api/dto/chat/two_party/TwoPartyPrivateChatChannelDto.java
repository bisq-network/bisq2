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

package bisq.api.dto.chat.two_party;

import bisq.api.dto.chat.ChatChannelDomainDto;
import bisq.api.dto.user.profile.UserProfileDto;

/**
 * A two-party private chat channel (a DM with one peer).
 * <p>
 * Carries {@code myUserProfile} rather than the channel's {@code UserIdentity}, unlike
 * {@link bisq.api.dto.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelDto}: a client only ever
 * renders the profile, and the identity would put key material on the wire for nothing.
 * <p>
 * {@code unreadCount} comes from {@code ChatNotificationService} and is the one mutable value here,
 * which is why the channel is re-sent whenever it changes rather than getting a topic of its own.
 */
public record TwoPartyPrivateChatChannelDto(String id,
                                            ChatChannelDomainDto chatChannelDomain,
                                            UserProfileDto peer,
                                            UserProfileDto myUserProfile,
                                            long unreadCount) {
}
