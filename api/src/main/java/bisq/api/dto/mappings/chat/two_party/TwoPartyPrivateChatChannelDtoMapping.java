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
import bisq.api.dto.DtoMappings.UserProfileMapping;
import bisq.api.dto.chat.two_party.TwoPartyPrivateChatChannelDto;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.user.profile.UserProfile;

public class TwoPartyPrivateChatChannelDtoMapping {
    // toBisq2Model not provided as we don't have the mutable data in the dto

    /**
     * @param peer        the re-resolved peer profile. The one persisted inside the channel is a
     *                    snapshot which goes stale when the peer renames themselves, so the caller
     *                    looks it up rather than us reading {@code channel.getPeer()} here.
     * @param unreadCount from {@code ChatNotificationService}, which the channel does not know about
     */
    public static TwoPartyPrivateChatChannelDto fromBisq2Model(TwoPartyPrivateChatChannel value,
                                                               UserProfile peer,
                                                               long unreadCount) {
        return new TwoPartyPrivateChatChannelDto(
                value.getId(),
                ChatChannelDomainMapping.fromBisq2Model(value.getChatChannelDomain()),
                UserProfileMapping.fromBisq2Model(peer),
                UserProfileMapping.fromBisq2Model(value.getMyUserIdentity().getUserProfile()),
                unreadCount
        );
    }
}
