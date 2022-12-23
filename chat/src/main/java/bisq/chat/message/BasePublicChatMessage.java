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

package bisq.chat.message;

import bisq.common.encoding.Hex;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * PublicChatMessage is added as public data to the distributed network storage.
 */
@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class BasePublicChatMessage extends ChatMessage implements DistributedData {
    protected BasePublicChatMessage(String messageId,
                                    String channelId,
                                    String authorId,
                                    Optional<String> text,
                                    Optional<Quotation> quotation,
                                    long date,
                                    boolean wasEdited,
                                    MessageType messageType,
                                    MetaData metaData) {
        super(messageId, channelId, authorId, text, quotation, date, wasEdited, messageType, metaData);
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        // AuthorId must be pubKeyHash. We get pubKeyHash passed from the data storage layer where the signature is 
        // verified as well, so we can be sure it's the sender of the message. This check prevents against 
        // impersonation attack.
        return !authorId.equals(Hex.encode(pubKeyHash));
    }
}