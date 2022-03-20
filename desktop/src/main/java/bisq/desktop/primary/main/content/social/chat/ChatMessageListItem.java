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
import bisq.common.util.StringUtils;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.FilteredListItem;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.social.chat.ChatMessage;
import javafx.scene.image.Image;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@EqualsAndHashCode
class ChatMessageListItem implements Comparable<ChatMessageListItem>, FilteredListItem {
    private static Map<ByteArray, Image> iconImageCache = new HashMap<>();

    private final ChatMessage chatMessage;
    private final String message;
    private final String senderUserName;
    private final String time;
    private final String date;
    private final ByteArray pubKeyHash;

    public ChatMessageListItem(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
        message = chatMessage.getText();
        senderUserName = chatMessage.getSenderUserName();
        time = TimeFormatter.formatTime(new Date(chatMessage.getDate()));
        date = DateFormatter.formatDateTime(new Date(chatMessage.getDate()));
        pubKeyHash = new ByteArray(chatMessage.getSenderNetworkId().getPubKey().publicKey().getEncoded());
    }

    public Image getIconImage() {
        if (!iconImageCache.containsKey(pubKeyHash)) {
            iconImageCache.put(pubKeyHash, RoboHash.getImage(pubKeyHash, false));
        }
        return iconImageCache.get(pubKeyHash);
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