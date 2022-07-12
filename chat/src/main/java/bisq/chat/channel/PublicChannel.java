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

package bisq.chat.channel;

import bisq.chat.ChannelNotificationType;
import bisq.chat.channel.Channel;
import bisq.chat.message.PublicChatMessage;
import bisq.common.observable.ObservableSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class PublicChannel<M extends PublicChatMessage> extends Channel<M> {
    // We do not persist the messages as they are persisted in the P2P data store.
    protected transient final ObservableSet<M> chatMessages = new ObservableSet<>();

    public PublicChannel(String id, ChannelNotificationType channelNotificationType) {
        super(id, channelNotificationType);
    }
}
