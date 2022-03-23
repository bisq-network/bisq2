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

package bisq.desktop.primary.main.content.social.chat;

import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.util.StringUtils;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.FilteredListItem;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.security.DigestUtil;
import bisq.social.chat.ChatMessage;
import bisq.social.chat.QuotedMessage;
import javafx.scene.image.Image;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Getter
@EqualsAndHashCode
class ChatMessageListItem implements Comparable<ChatMessageListItem>, FilteredListItem {
    private static final Map<ByteArray, Image> iconImageCache = new HashMap<>();

    private final ChatMessage chatMessage;
    private final String message;
    private final String senderUserName;
    private final String time;
    private final String date;
    private final String chatUserId;
    private final ByteArray pubKeyHashAsByteArray;
    private final Optional<QuotedMessage> quotedMessage;

    public ChatMessageListItem(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
        message = chatMessage.getText();
        quotedMessage = chatMessage.getQuotedMessage();
        senderUserName = chatMessage.getSenderUserName();
        time = TimeFormatter.formatTime(new Date(chatMessage.getDate()));
        date = DateFormatter.formatDateTime(new Date(chatMessage.getDate()));
        byte[] pubKeyHash = DigestUtil.hash(chatMessage.getSenderNetworkId().getPubKey().publicKey().getEncoded());
        pubKeyHashAsByteArray = new ByteArray(pubKeyHash);
        chatUserId = Hex.encode(pubKeyHash);
    }

    public Image getIconImage() {
        if (!iconImageCache.containsKey(pubKeyHashAsByteArray)) {
            iconImageCache.put(pubKeyHashAsByteArray, RoboHash.getImage(pubKeyHashAsByteArray, false));
        }
        return iconImageCache.get(pubKeyHashAsByteArray);
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
                StringUtils.containsIgnoreCase(senderUserName, filterString) ||
                StringUtils.containsIgnoreCase(date, filterString);
    }
}