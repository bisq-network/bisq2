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

package bisq.application;

import bisq.common.util.MathUtils;
import bisq.network.p2p.message.MessageCostPolicy;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MessageCostPolicy selects the values of a storable type by its class name. For a type without values, the messages
 * which carry it get a lower cost factor than intended. A new storable type needs its values in the policy.
 * <p>
 * The storage cost factors of the policy are constants. Nodes of versions up to 2.1.13 derive them from the MetaData
 * of the types, so the constants must match the MetaData of the types.
 */
class StorableTypeMessageCostTest {
    @Test
    void messageCostPolicyHasValuesForEveryStorableType() throws Exception {
        List<Class<?>> storableTypes = StorableTypes.find();
        // The scan must see the types of all modules. If it finds only a few, the classpath scan does not work.
        assertTrue(storableTypes.size() > 50, "Found only " + storableTypes.size() + " storable types");

        List<String> errors = new ArrayList<>();
        for (Class<?> type : storableTypes) {
            String className = type.getSimpleName();
            if (MessageCostPolicy.findStorageCostFactor(className).isEmpty()) {
                errors.add(type.getName() + " has no storage cost factor");
            }
            // Only authenticated and append-only data are readable for the receiver. A MailboxMessage is encrypted.
            boolean isReadableData = !MailboxMessage.class.isAssignableFrom(type);
            if (isReadableData && MessageCostPolicy.findDataCostFactor(className).isEmpty()) {
                errors.add(type.getName() + " has no data cost factor");
            }
        }
        assertEquals(List.of(), errors);
    }

    /**
     * If this test fails after a change of the MetaData of an existing type, keep the value of the policy. It is a
     * protocol constant (see docs/specifications/network/message-cost.md), and nodes of earlier versions compute it
     * from their own MetaData. Exclude the type from this test and give the reason. For a new type, add the value which
     * this test reports to the policy.
     */
    @Test
    void storageCostFactorsMatchTheMetaDataOfTheTypes() throws Exception {
        List<String> errors = new ArrayList<>();
        for (Class<?> type : StorableTypes.find()) {
            MetaData metaData = (MetaData) type.getDeclaredField("META_DATA").get(null);
            double derivedCostFactor = MathUtils.roundDouble(getStorageCostFactorOfEarlierVersions(metaData), 2);
            Optional<Double> costFactor = MessageCostPolicy.findStorageCostFactor(type.getSimpleName());
            // A missing value is reported by messageCostPolicyHasValuesForEveryStorableType
            if (costFactor.isPresent() && Double.compare(costFactor.get(), derivedCostFactor) != 0) {
                errors.add(type.getName() + ": the policy has " + costFactor.get() +
                        ", its MetaData gives " + derivedCostFactor);
            }
        }
        assertEquals(List.of(), errors);
    }

    // The formula of MetaData.getCostFactor() in versions up to 2.1.13
    private static double getStorageCostFactorOfEarlierVersions(MetaData metaData) {
        double ttlImpact = MathUtils.bounded(0, 1, metaData.getTtl() / (double) TimeUnit.DAYS.toMillis(100));
        double mapSizeImpact = MathUtils.bounded(0, 1, metaData.getMaxMapSize() / 10_000d);
        return MathUtils.bounded(0, 1, ttlImpact + mapSizeImpact);
    }
}
