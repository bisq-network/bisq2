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

package bisq.network.p2p.message;

import bisq.common.util.MathUtils;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;

/**
 * Defines the cost factor of messages. A peer authorizes a message only if the sender paid a cost which grows with the
 * cost factor of the message, for example the difficulty of a proof of work. The policy does not depend on the method
 * which pays the cost.
 */
public final class MessageCostPolicy {
    private MessageCostPolicy() {
    }

    /**
     * The cost factor of a message which a peer sends to another peer over a connection.
     */
    public static double getCostFactor(EnvelopePayloadMessage message) {
        return message.getCostFactor();
    }

    /**
     * The part of the cost factor which depends on the storage properties of a storable type. Data which the network
     * storage keeps longer, or of which it may keep more entries, costs more.
     */
    public static double getStorageCostFactor(MetaData metaData) {
        double ttlImpact = MathUtils.bounded(0, 1, metaData.getTtl() / (double) MetaData.TTL_100_DAYS);
        double mapSizeImpact = MathUtils.bounded(0, 1, metaData.getMaxMapSize() / (double) MetaData.MAX_MAP_SIZE_10_000);
        double impact = ttlImpact + mapSizeImpact;
        return MathUtils.bounded(0, 1, impact);
    }

    /**
     * The authorization needs the cost factor of a request before StorageService checks the class name. A type which is
     * not registered (e.g. added in a later version) is not stored, so its MetaData has no impact on the cost.
     * Otherwise the authorization could reject messages of newer nodes.
     *
     * @return the storage cost factor of the registered MetaData, or 0 if the class name is not registered.
     */
    public static double getStorageCostFactor(String className) {
        return StorableTypesRegistry.findMetaData(className)
                .map(MessageCostPolicy::getStorageCostFactor)
                .orElse(0d);
    }
}
