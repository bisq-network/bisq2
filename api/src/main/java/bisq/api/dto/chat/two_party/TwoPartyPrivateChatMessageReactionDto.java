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
import bisq.api.dto.network.identity.NetworkIdDto;
import bisq.api.dto.user.profile.UserProfileDto;

/**
 * Field-identical to {@link bisq.api.dto.chat.reactions.BisqEasyOpenTradeMessageReactionDto}, because
 * upstream both reaction types add nothing to {@code PrivateChatMessageReaction}. They stay separate
 * because they are distinct network types.
 */
public record TwoPartyPrivateChatMessageReactionDto(String id,
                                                    UserProfileDto senderUserProfile,
                                                    String receiverUserProfileId,
                                                    NetworkIdDto receiverNetworkId,
                                                    String chatChannelId,
                                                    ChatChannelDomainDto chatChannelDomain,
                                                    String chatMessageId,
                                                    int reactionId,
                                                    long date,
                                                    boolean isRemoved) {
}
