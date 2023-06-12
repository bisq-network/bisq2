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

import bisq.i18n.Res;
import bisq.offer.Offer;
import com.google.common.base.Joiner;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SettlementFormatter {
    public static String asQuoteSideSettlementMethodsString(List<String> methodNames) {
        return asString(SettlementUtil.createQuoteSideSpecsFromMethodNames(methodNames));
    }

    public static String asQuoteSideSettlementMethodsString(Offer offer) {
        return asString(offer.getQuoteSideSettlementSpecs());
    }

    private static List<String> getDisplayStringList(Collection<SettlementSpec> settlementSpecs) {
        return SettlementUtil.getSettlementMethodNames(settlementSpecs).stream()
                .map(methodName -> {
                    if (Res.has(methodName)) {
                        return Res.get(methodName);
                    } else {
                        return methodName;
                    }
                })
                .sorted()
                .collect(Collectors.toList());
    }

    private static String asString(List<SettlementSpec> settlementSpecs) {
        return Joiner.on(", ").join(getDisplayStringList(settlementSpecs));
    }
}