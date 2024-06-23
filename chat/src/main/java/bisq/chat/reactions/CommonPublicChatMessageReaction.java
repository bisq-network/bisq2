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

package bisq.chat.reactions;

import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import com.google.protobuf.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public class CommonPublicChatMessageReaction implements DistributedData {
    @EqualsAndHashCode.Exclude
    private final MetaData metaData = new MetaData(MetaData.TTL_10_DAYS, MetaData.LOW_PRIORITY, getClass().getSimpleName(), MetaData.MAX_MAP_SIZE_10_000);

    @Override
    public MetaData getMetaData() {
        return null;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
    }

    @Override
    public double getCostFactor() {
        return 0.5;
    }

    @Override
    public void verify() {
    }

    @Override
    public Message.Builder getBuilder(boolean serializeForHash) {
        return null;
    }

    @Override
    public Message toProto(boolean serializeForHash) {
        return null;
    }

    // API
    //getReactionsForMessage(String messageId, String channelId)
}
