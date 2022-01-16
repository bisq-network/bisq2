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

import bisq.network.NetworkId;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

@Getter
@ToString
@EqualsAndHashCode
public class ChatMessage implements MailboxMessage {
    private final String channelId;
    private final String text;
    private final String senderUserName;
    private final NetworkId senderNetworkId;
    private final long date;
    private final ChannelType channelType;
    private final PrivateChannel.Context context;
    private final MetaData metaData;

    //just temp for dev
    public ChatMessage(String channelId, String text, String senderUserName, NetworkId senderNetworkId,
                       long date, ChannelType channelType, PrivateChannel.Context context) {
        this.channelId = channelId;
        this.text = text;
        this.senderUserName = senderUserName;
        this.senderNetworkId = senderNetworkId;
        this.date = date;
        this.channelType = channelType;
        this.context = context;
        metaData = new MetaData(TimeUnit.DAYS.toMillis(10), 100000, getClass().getSimpleName());
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }
}