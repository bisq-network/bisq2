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

import bisq.api.dto.chat.ChatMessageTypeDto;
import bisq.api.dto.chat.CitationDto;
import bisq.api.dto.network.identity.NetworkIdDto;
import bisq.api.dto.user.profile.UserProfileDto;

import java.util.Optional;
import java.util.Set;

/**
 * A message in a two-party private chat (DM). This is
 * {@link bisq.api.dto.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto} without the trade
 * fields, mirroring upstream where {@code TwoPartyPrivateChatMessage} adds nothing at all to
 * {@code PrivateChatMessage}.
 * <p>
 * {@code citationAuthorUserProfile} is resolved by the node, not carried by the message itself: the
 * citation only holds an author id, and only the node can look it up.
 */
public record TwoPartyPrivateChatMessageDto(String messageId,
                                            String channelId,
                                            UserProfileDto senderUserProfile,
                                            String receiverUserProfileId,
                                            NetworkIdDto receiverNetworkId,
                                            Optional<String> text,
                                            Optional<CitationDto> citation,
                                            long date,
                                            ChatMessageTypeDto chatMessageType,
                                            Set<TwoPartyPrivateChatMessageReactionDto> chatMessageReactions,
                                            Optional<UserProfileDto> citationAuthorUserProfile) {
}
