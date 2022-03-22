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

package bisq.social.chat;

import bisq.social.user.ChatUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class PublicChannel extends Channel<PublicChatMessage> {
    private final String channelName;
    private final String description;
    private final ChatUser channelAdmin;
    private final Set<ChatUser> channelModerators;

    public PublicChannel(String id,
                         String channelName,
                         String description,
                         ChatUser channelAdmin,
                         Set<ChatUser> channelModerators) {
        super(id);

        this.channelName = channelName;
        this.description = description;
        this.channelAdmin = channelAdmin;
        this.channelModerators = channelModerators;
    }
}