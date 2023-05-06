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

package bisq.desktop.notifications.chat;

import bisq.chat.channel.ChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.PrivateChatMessage;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Getter
@EqualsAndHashCode
public class ChatNotification<T extends ChatMessage> implements Comparable<ChatNotification<T>> {
    private final ChatChannel<? extends ChatMessage> chatChannel;
    private final T chatMessage;
    private final String message;
    private final Optional<UserProfile> senderUserProfile;
    private final String userName;

    public ChatNotification(ChatChannel<? extends ChatMessage> chatChannel, T chatMessage, UserProfileService userProfileService) {
        this.chatChannel = chatChannel;
        this.chatMessage = chatMessage;

        if (chatMessage instanceof PrivateChatMessage) {
            senderUserProfile = Optional.of(((PrivateChatMessage) chatMessage).getSender());
        } else {
            senderUserProfile = userProfileService.findUserProfile(chatMessage.getAuthorId());
        }
        message = chatMessage.getText();

        userName = senderUserProfile.map(UserProfile::getUserName).orElse(Res.get("na"));
    }

    @Override
    public int compareTo(ChatNotification o) {
        return new Date(chatMessage.getDate()).compareTo(new Date(o.getChatMessage().getDate()));
    }
}