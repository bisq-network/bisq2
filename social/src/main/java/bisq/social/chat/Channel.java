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

import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Channel {
    public enum ChannelType implements Serializable {
        BTC_EUR,
        BTC_USD,
        PUBLIC,
        PRIVATE
    }

    private final ChannelType channelType;
    private final String id;
    private final String name;
    private final Set<ChatUser> members = new HashSet<>();
    private final List<ChatEntry> messages = new ArrayList<>();

    public Channel(Channel.ChannelType channelType, String id, String name) {
        this.channelType = channelType;
        this.id = id;
        this.name = name;
    }

    public void addMember(ChatUser chatUser) {
        members.add(chatUser);
    }

    public void addMessages(ChatEntry chatEntry) {
        if (!messages.contains(chatEntry)) {
            messages.add(chatEntry);
        }
    }
}