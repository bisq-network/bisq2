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

package bisq.account.payment_method.stable_coin;

import bisq.common.asset.StableCoin;
import bisq.common.asset.StableCoinRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StableCoinPaymentRailUtil {

    public static List<StableCoinPaymentRail> getPaymentRails() {
        return List.of(StableCoinPaymentRail.values());
    }

    public static Optional<StableCoinPaymentRail> find(StableCoin stableCoin) {
        return getPaymentRails().stream()
                .filter(rail -> rail.getStableCoin().equals(stableCoin))
                .findAny();
    }

    public static List<StableCoinPaymentRail> getPaymentRails(String code) {
        return getPaymentRails().stream()
                .filter(paymentRail -> paymentRail.getStableCoin().getCode().equals(code))
                .collect(Collectors.toList());
    }

    public static List<StableCoinPaymentRail> getMajorStableCoinPaymentRails() {
        return StableCoinRepository.getMajorStableCoins().stream()
                .flatMap(asset -> StableCoinPaymentRailUtil.find(asset).stream())
                .collect(Collectors.toList());
    }
}