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

package bisq.bonded_roles.security_manager.alert;

import bisq.bonded_roles.release.AppType;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class AuthorizedAlertDataUtils {
    public static final Comparator<AuthorizedAlertData> RELEVANCE_COMPARATOR =
            Comparator.comparing(AuthorizedAlertData::getAlertType)
                    .thenComparing(AuthorizedAlertData::getDate);

    private AuthorizedAlertDataUtils() {
    }

    /**
     * Returns the single most important active trade-restricting alert for the given app type.
     *
     * <p>Selection policy:
     * <ol>
     *   <li>If any {@code haltTrading} alert is present, the most-recent one is returned —
     *       halt-trading takes precedence over a version requirement, even when that halt alert
     *       is older. There is no point in showing a min-version requirement when trading is
     *       already halted.</li>
     *   <li>Otherwise, the most-recent {@code requireVersionForTrading} alert is returned.</li>
     * </ol>
     */
    public static Optional<AuthorizedAlertData> findMostRecentTradeRestrictingAlert(
            Stream<AuthorizedAlertData> alerts, AppType appType) {
        List<AuthorizedAlertData> candidates = alerts
                .filter(AuthorizedAlertData::isTradeRestrictingAlert)
                .filter(alert -> alert.getAppType() == appType)
                .toList();

        Optional<AuthorizedAlertData> mostRecentHalt =
                findMostRecentAlert(candidates, AuthorizedAlertData::isHaltTrading);
        if (mostRecentHalt.isPresent()) {
            return mostRecentHalt;
        }
        return findMostRecentAlert(candidates, AuthorizedAlertData::isRequireVersionForTrading);
    }

    private static Optional<AuthorizedAlertData> findMostRecentAlert(
            List<AuthorizedAlertData> alerts, Predicate<AuthorizedAlertData> criterion) {
        return alerts.stream()
                .filter(criterion)
                .max(Comparator.comparingLong(AuthorizedAlertData::getDate));
    }
}