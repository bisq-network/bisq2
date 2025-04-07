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

package bisq.dto.chat.bisq_easy.open_trades;

import bisq.dto.chat.ChatMessageTypeDto;
import bisq.dto.chat.CitationDto;
import bisq.dto.chat.reactions.BisqEasyOpenTradeMessageReactionDto;
import bisq.dto.network.identity.NetworkIdDto;
import bisq.dto.offer.bisq_easy.BisqEasyOfferDto;
import bisq.dto.user.profile.UserProfileDto;

import java.util.Optional;
import java.util.Set;

public record BisqEasyOpenTradeMessageDto(String tradeId,
                                          String messageId,
                                          String channelId,
                                          UserProfileDto senderUserProfile,
                                          String receiverUserProfileId,
                                          NetworkIdDto receiverNetworkId,
                                          Optional<String> text,
                                          Optional<CitationDto> citation,
                                          long date,
                                          Optional<UserProfileDto> mediator,
                                          ChatMessageTypeDto chatMessageType,
                                          Optional<BisqEasyOfferDto> bisqEasyOffer,
                                          Set<BisqEasyOpenTradeMessageReactionDto> chatMessageReactions
                                          ) {
}