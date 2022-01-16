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

@Getter
public class PrivateChannel extends Channel {
    public enum Context {
        PUBLIC_CHAT,
        TRADE_INTENT
    }

    private final ChatPeer chatPeer;
    private final ChatIdentity chatIdentity;
    private final Context context;

    public PrivateChannel(String id, ChatPeer chatPeer, ChatIdentity chatIdentity, Context context) {
        super(id);
        this.chatPeer = chatPeer;
        this.chatIdentity = chatIdentity;
        this.context = context;
    }

    public String getChannelName() {
        return chatPeer.userName();
    }
}