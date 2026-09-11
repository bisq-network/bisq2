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

import bisq.api.dto.chat.ChatMessageTypeDto;
import bisq.api.dto.chat.CitationDto;
import bisq.api.dto.user.profile.UserProfileDto;

import java.util.Optional;
import java.util.Set;

/**
 * The author is carried both ways: as an id, which is what the domain message holds and what a client
 * can key its own profile cache by, and as the resolved profile, so a client that has no profile store
 * of its own can render the message without a second call.
 */
public record CommonPublicChatMessageDto(String messageId,
                                         String channelId,
                                         String authorUserProfileId,
                                         UserProfileDto authorUserProfile,
                                         Optional<String> text,
                                         Optional<CitationDto> citation,
                                         Optional<UserProfileDto> citationAuthorUserProfile,
                                         long date,
                                         ChatMessageTypeDto chatMessageType,
                                         boolean wasEdited,
                                         Set<CommonPublicChatMessageReactionDto> chatMessageReactions) {
}
