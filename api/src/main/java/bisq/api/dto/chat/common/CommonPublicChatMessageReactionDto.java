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

package bisq.api.dto.chat.common;

import bisq.api.dto.chat.ChatChannelDomainDto;
import bisq.api.dto.user.profile.UserProfileDto;

/**
 * No {@code isRemoved}, unlike the private chat reaction dto: a public reaction is removed from the
 * network for real, so its removal reaches the client as a {@code REMOVED} event, not as a marker.
 * The sender is carried as an id as well as a resolved profile, for the reason on
 * {@link CommonPublicChatMessageDto}.
 */
public record CommonPublicChatMessageReactionDto(String id,
                                                 String senderUserProfileId,
                                                 UserProfileDto senderUserProfile,
                                                 String chatChannelId,
                                                 ChatChannelDomainDto chatChannelDomain,
                                                 String chatMessageId,
                                                 int reactionId,
                                                 long date) {
}
