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

package bisq.api.dto.mappings.chat.common;

import bisq.api.dto.DtoMappings.ChatChannelDomainMapping;
import bisq.api.dto.DtoMappings.UserProfileMapping;
import bisq.api.dto.chat.common.CommonPublicChatMessageReactionDto;
import bisq.chat.reactions.CommonPublicChatMessageReaction;
import bisq.user.profile.UserProfile;

public class CommonPublicChatMessageReactionDtoMapping {
    // toBisq2Model not provided: nothing consumes it, matching the sibling mappings in this package.

    /**
     * @param sender the resolved sender profile. A public reaction only carries the sender's id, so the
     *               caller looks the profile up and decides what to do when it cannot be found.
     */
    public static CommonPublicChatMessageReactionDto fromBisq2Model(CommonPublicChatMessageReaction value,
                                                                    UserProfile sender) {
        return new CommonPublicChatMessageReactionDto(
                value.getId(),
                value.getUserProfileId(),
                UserProfileMapping.fromBisq2Model(sender),
                value.getChatChannelId(),
                ChatChannelDomainMapping.fromBisq2Model(value.getChatChannelDomain()),
                value.getChatMessageId(),
                value.getReactionId(),
                value.getDate()
        );
    }
}
