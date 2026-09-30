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

package bisq.api.dto.mappings.chat.two_party;

import bisq.api.dto.DtoMappings.ChatMessageTypeMapping;
import bisq.api.dto.DtoMappings.CitationMapping;
import bisq.api.dto.DtoMappings.NetworkIdMapping;
import bisq.api.dto.DtoMappings.UserProfileMapping;
import bisq.api.dto.chat.two_party.TwoPartyPrivateChatMessageDto;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class TwoPartyPrivateChatMessageDtoMapping {
    // toBisq2Model not provided: nothing consumes it, and the dto carries no chatChannelDomain,
    // so building the Bisq 2 model would mean inventing one.

    /**
     * @param visibleReactions the message's reactions the caller wants on the wire; the caller owns the
     *                         ban and removal rules, this only maps what it was handed.
     */
    public static TwoPartyPrivateChatMessageDto fromBisq2Model(
            TwoPartyPrivateChatMessage value,
            Optional<UserProfileDto> citationAuthorUserProfile,
            Collection<TwoPartyPrivateChatMessageReaction> visibleReactions) {
        return new TwoPartyPrivateChatMessageDto(
                value.getId(),
                value.getChannelId(),
                UserProfileMapping.fromBisq2Model(value.getSenderUserProfile()),
                value.getReceiverUserProfileId(),
                NetworkIdMapping.fromBisq2Model(value.getReceiverNetworkId()),
                value.getText(),
                value.getCitation().map(CitationMapping::fromBisq2Model),
                value.getDate(),
                ChatMessageTypeMapping.fromBisq2Model(value.getChatMessageType()),
                visibleReactions.stream()
                        .map(TwoPartyPrivateChatMessageReactionDtoMapping::fromBisq2Model)
                        .collect(Collectors.toSet()),
                citationAuthorUserProfile
        );
    }
}
