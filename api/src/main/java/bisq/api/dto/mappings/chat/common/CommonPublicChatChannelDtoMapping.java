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
import bisq.api.dto.chat.common.CommonPublicChatChannelDto;
import bisq.chat.common.CommonPublicChatChannel;

public class CommonPublicChatChannelDtoMapping {
    // toBisq2Model not provided: nothing consumes it, matching the private chat channel mapping.

    /**
     * Known limitation: the title and description can describe a different channel than the id names.
     * {@code getId()} migrates, {@code getDisplayString()} and {@code getDescription()} read the raw
     * fields, so on a node whose surviving channel is a deprecated sub-domain — a store written before
     * the consolidation keeps exactly one of the merged channels, and which one is the order they were
     * persisted in — the id is {@code discussion.bisq} while the title is the one of whichever channel
     * survived, which on such a node is likely one of the three that were merged into it.
     * <p>
     * Left as the domain has it, which is also what desktop shows. 85a71c5472 made the migration one of
     * identity only and, in the same commit, had {@code CommonChatTabController} drop deprecated channels
     * rather than render them — so the chat tabs never hit this. The profile card does:
     * {@code ProfileCardMessagesController} iterates the channels unfiltered and
     * {@code ChannelMessagesDisplayList} heads the list with {@code getDisplayString()}, so it heads that
     * node's messages with the same wrong title today. That tab arrived in 877358feee, months after the
     * consolidation.
     * <p>
     * So this is desktop's behaviour rather than a divergence introduced here, and resolving the strings
     * in a dto mapper would fix one surface of a shared problem while putting i18n key construction where
     * it does not belong. Whoever confirms a store like that still exists should fix
     * {@code getDisplayString()} for both.
     *
     * @param unreadCount from {@code ChatNotificationService}, which the channel does not know about
     */
    public static CommonPublicChatChannelDto fromBisq2Model(CommonPublicChatChannel value, long unreadCount) {
        return new CommonPublicChatChannelDto(
                value.getId(),
                ChatChannelDomainMapping.fromBisq2Model(value.getChatChannelDomain()),
                value.getDisplayString(),
                value.getDescription(),
                unreadCount
        );
    }
}
