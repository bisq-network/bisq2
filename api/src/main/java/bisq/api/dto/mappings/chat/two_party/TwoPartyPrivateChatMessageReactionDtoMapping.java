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

import bisq.api.dto.DtoMappings.ChatChannelDomainMapping;
import bisq.api.dto.DtoMappings.NetworkIdMapping;
import bisq.api.dto.DtoMappings.UserProfileMapping;
import bisq.api.dto.chat.two_party.TwoPartyPrivateChatMessageReactionDto;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;

public class TwoPartyPrivateChatMessageReactionDtoMapping {
    // toBisq2Model not provided: nothing consumes it, matching the two sibling mappings in this package.

    public static TwoPartyPrivateChatMessageReactionDto fromBisq2Model(TwoPartyPrivateChatMessageReaction value) {
        return new TwoPartyPrivateChatMessageReactionDto(
                value.getId(),
                UserProfileMapping.fromBisq2Model(value.getSenderUserProfile()),
                value.getReceiverUserProfileId(),
                NetworkIdMapping.fromBisq2Model(value.getReceiverNetworkId()),
                value.getChatChannelId(),
                ChatChannelDomainMapping.fromBisq2Model(value.getChatChannelDomain()),
                value.getChatMessageId(),
                value.getReactionId(),
                value.getDate(),
                value.isRemoved()
        );
    }
}
