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

import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;

import java.util.Optional;

public abstract class PublicChatMessage extends ChatMessage implements DistributedData {
    protected PublicChatMessage(String channelId,
                                String authorId,
                                Optional<String> text,
                                Optional<Quotation> quotation,
                                long date,
                                boolean wasEdited,
                                MetaData metaData) {
        super(channelId, authorId, text, quotation, date, wasEdited, metaData);
    }
}