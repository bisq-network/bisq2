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

package bisq.desktop.primary.main.content.social.onboarded.chat;

import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.util.StringUtils;
import bisq.desktop.components.table.FilteredListItem;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.security.DigestUtil;
import bisq.social.chat.ChatMessage;
import bisq.social.chat.QuotedMessage;
import bisq.social.user.ChatUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Getter
@EqualsAndHashCode
public class ChatMessageListItem<T extends ChatMessage> implements Comparable<ChatMessageListItem<T>>, FilteredListItem {
    private final T chatMessage;
    private final String message;
    private final String authorUserName;
    private final String time;
    private final String date;
    private final String chatUserId;
    private final ByteArray pubKeyHashAsByteArray;
    private final Optional<QuotedMessage> quotedMessage;
    private final ChatUser author;

    public ChatMessageListItem(T chatMessage) {
        this.chatMessage = chatMessage;
        String editPostFix = chatMessage.isWasEdited() ? ChatView.EDITED_POST_FIX : "";
        message = chatMessage.getText() + editPostFix;
        quotedMessage = chatMessage.getQuotedMessage();
        author = chatMessage.getAuthor();
        authorUserName = author.getUserName();
        time = TimeFormatter.formatTime(new Date(chatMessage.getDate()));
        date = DateFormatter.formatDateTime(new Date(chatMessage.getDate()));
        byte[] pubKeyHash = DigestUtil.hash(author.getNetworkId().getPubKey().publicKey().getEncoded());
        pubKeyHashAsByteArray = new ByteArray(pubKeyHash);
        chatUserId = Hex.encode(pubKeyHash);
    }

    @Override
    public int compareTo(ChatMessageListItem o) {
        return new Date(chatMessage.getDate()).compareTo(new Date(o.getChatMessage().getDate()));
    }

    @Override
    public boolean match(String filterString) {
        return filterString == null ||
                filterString.isEmpty() ||
                StringUtils.containsIgnoreCase(message, filterString) ||
                StringUtils.containsIgnoreCase(authorUserName, filterString) ||
                StringUtils.containsIgnoreCase(date, filterString);
    }
}