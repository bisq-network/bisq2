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

import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import lombok.Getter;

import java.io.Serializable;

@Getter
public abstract class Channel<T extends ChatMessage> implements Serializable {
    protected final String id;
    protected final ObservableSet<ChatMessage> chatMessages = new ObservableSet<>();
    protected final Observable<NotificationSetting> notificationSetting = new Observable<>(NotificationSetting.MENTION);

    public Channel(String id) {
        this.id = id;
    }

    public void addChatMessage(T chatMessage) {
        chatMessages.add(chatMessage);
    }

    public void removeChatMessage(T chatMessage) {
        chatMessages.remove(chatMessage);
    }

    public abstract String getChannelName();
}