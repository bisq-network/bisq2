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

package bisq.offer.settlement;

import bisq.account.payment.BitcoinPayment;
import bisq.offer.Offer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class SettlementUtil {
    public static List<SettlementSpec> createBaseSideSpecsForBitcoinMainChain() {
        return List.of(new SettlementSpec(BitcoinPayment.Method.MAINCHAIN.name()));
    }

    public static List<SettlementSpec> createQuoteSideSpecsFromMethodNames(List<String> paymentMethodNames) {
        checkArgument(!paymentMethodNames.isEmpty());
        return paymentMethodNames.stream()
                .map(SettlementSpec::new)
                .collect(Collectors.toList());
    }


    public static List<String> getSettlementMethodNames(Collection<SettlementSpec> settlementSpecs) {
        return settlementSpecs.stream()
                .map(SettlementSpec::getSettlementMethodName)
                .sorted()
                .collect(Collectors.toList());
    }


    public static List<String> getBaseSideSettlementMethodNames(Offer offer) {
        return getSettlementMethodNames(offer.getBaseSideSettlementSpecs());
    }

    public static List<String> getQuoteSideSettlementMethodNames(Offer offer) {
        return getSettlementMethodNames(offer.getQuoteSideSettlementSpecs());
    }
}