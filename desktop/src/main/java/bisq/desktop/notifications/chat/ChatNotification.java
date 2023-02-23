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

import bisq.chat.channel.Channel;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.PrivateChatMessage;
import bisq.chat.trade.priv.PrivateTradeChatMessage;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;

import static bisq.desktop.primary.main.content.components.ChatMessagesComponent.View.EDITED_POST_FIX;

@Slf4j
@Getter
@EqualsAndHashCode
public class ChatNotification<T extends ChatMessage> implements Comparable<ChatNotification<T>> {
    private final Channel<? extends ChatMessage> channel;
    private final T chatMessage;
    private final String message;
    private final Optional<UserProfile> senderUserProfile;
    private final String nym;
    private final String nickName;

    public ChatNotification(Channel<? extends ChatMessage> channel, T chatMessage, UserProfileService userProfileService) {
        this.channel = channel;
        this.chatMessage = chatMessage;

        if (chatMessage instanceof PrivateTradeChatMessage) {
            senderUserProfile = Optional.of(((PrivateTradeChatMessage) chatMessage).getSender());
        } else if (chatMessage instanceof PrivateChatMessage) {
            senderUserProfile = Optional.of(((PrivateChatMessage) chatMessage).getSender());
        } else {
            senderUserProfile = userProfileService.findUserProfile(chatMessage.getAuthorId());
        }
        String editPostFix = chatMessage.isWasEdited() ? EDITED_POST_FIX : "";
        message = chatMessage.getText() + editPostFix;

        nym = senderUserProfile.map(UserProfile::getNym).orElse("");
        nickName = senderUserProfile.map(UserProfile::getNickName).orElse("");
    }

    @Override
    public int compareTo(ChatNotification o) {
        return new Date(chatMessage.getDate()).compareTo(new Date(o.getChatMessage().getDate()));
    }
}