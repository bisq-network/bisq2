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

package bisq.account.payment_method.cbdc;

import bisq.common.asset.Cbdc;
import bisq.common.asset.CbdcRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CbdcPaymentRailUtil {
    public static List<CbdcPaymentRail> getPaymentRails() {
        return List.of(CbdcPaymentRail.values());
    }

    public static Optional<CbdcPaymentRail> find(Cbdc cbdc) {
        return getPaymentRails().stream()
                .filter(rail -> rail.getCbdc().equals(cbdc))
                .findAny();
    }

    public static List<CbdcPaymentRail> getPaymentRails(String code) {
        return getPaymentRails().stream()
                .filter(paymentRail -> paymentRail.getCbdc().getCode().equals(code))
                .collect(Collectors.toList());
    }

    public static List<CbdcPaymentRail> getMajorCbdcPaymentRails() {
        return CbdcRepository.getMajorCbdcs().stream()
                .flatMap(asset -> CbdcPaymentRailUtil.find(asset).stream())
                .collect(Collectors.toList());
    }
}