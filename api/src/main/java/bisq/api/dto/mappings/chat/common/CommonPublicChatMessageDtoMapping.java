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

import bisq.api.dto.DtoMappings.ChatMessageTypeMapping;
import bisq.api.dto.DtoMappings.CitationMapping;
import bisq.api.dto.DtoMappings.UserProfileMapping;
import bisq.api.dto.chat.common.CommonPublicChatMessageDto;
import bisq.api.dto.chat.common.CommonPublicChatMessageReactionDto;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.user.profile.UserProfile;

import java.util.Optional;
import java.util.Set;

public class CommonPublicChatMessageDtoMapping {
    // toBisq2Model not provided: nothing consumes it, matching the sibling mappings in this package.

    /**
     * @param author                    the resolved author profile; the dto carries its id too
     * @param citationAuthorUserProfile the resolved author of the citation, if there is one and it resolves
     * @param visibleReactions          the reactions the caller wants on the wire, already mapped; the
     *                                  caller owns the visibility rules, this only assembles the dto
     */
    public static CommonPublicChatMessageDto fromBisq2Model(CommonPublicChatMessage value,
                                                            UserProfile author,
                                                            Optional<UserProfileDto> citationAuthorUserProfile,
                                                            Set<CommonPublicChatMessageReactionDto> visibleReactions) {
        return new CommonPublicChatMessageDto(
                value.getId(),
                value.getChannelId(),
                value.getAuthorUserProfileId(),
                UserProfileMapping.fromBisq2Model(author),
                value.getText(),
                value.getCitation().map(CitationMapping::fromBisq2Model),
                citationAuthorUserProfile,
                value.getDate(),
                ChatMessageTypeMapping.fromBisq2Model(value.getChatMessageType()),
                value.isWasEdited(),
                visibleReactions
        );
    }
}
