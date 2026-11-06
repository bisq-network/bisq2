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
import bisq.common.platform.Version;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
public final class AuthorizedAlertDataUtils {
    /**
     * The app types of the nodes which make offers. A new app type which makes offers must be added here.
     */
    public static final Set<AppType> MAKER_APP_TYPES = Set.of(AppType.DESKTOP, AppType.MOBILE_NODE);

    public static final Comparator<AuthorizedAlertData> RELEVANCE_COMPARATOR =
            Comparator.comparing(AuthorizedAlertData::getAlertType)
                    .thenComparing(AuthorizedAlertData::getDate);

    private AuthorizedAlertDataUtils() {
    }

    /**
     * An alert applies to the given app type when both sides match, or when either side is
     * {@link AppType#UNSPECIFIED} (wildcard). A security manager publishing a network-wide
     * alert without an app type must restrict every app.
     */
    public static boolean appliesTo(AuthorizedAlertData alert, AppType appType) {
        return appType == AppType.UNSPECIFIED
                || alert.getAppType() == AppType.UNSPECIFIED
                || alert.getAppType() == appType;
    }

    /**
     * True while any applicable EMERGENCY alert has haltTrading set.
     */
    public static boolean isTradingHalted(Stream<AuthorizedAlertData> alerts, AppType appType) {
        return alerts
                .filter(alert -> alert.getAlertType() == AlertType.EMERGENCY)
                .filter(alert -> appliesTo(alert, appType))
                .anyMatch(AuthorizedAlertData::isHaltTrading);
    }

    /**
     * The min. version required for trading from the most recent applicable EMERGENCY alert.
     * Alerts with requireVersionForTrading but no minVersion are malformed and ignored, so they
     * cannot shadow an older well-formed restriction.
     */
    public static Optional<String> findMinRequiredVersionForTrading(Stream<AuthorizedAlertData> alerts, AppType appType) {
        return alerts
                .filter(alert -> alert.getAlertType() == AlertType.EMERGENCY)
                .filter(alert -> appliesTo(alert, appType))
                .filter(AuthorizedAlertData::isRequireVersionForTrading)
                .filter(alert -> alert.getMinVersion().isPresent())
                .max(Comparator.comparingLong(AuthorizedAlertData::getDate))
                .flatMap(AuthorizedAlertData::getMinVersion);
    }

    /**
     * The highest min. version required for trading over all {@link #MAKER_APP_TYPES}. The taker does not know the
     * app type of the maker, so it checks the maker with the strictest restriction. A min. version which cannot be
     * compared, for example with a part which does not fit into an int, wins, so that the taker refuses every maker
     * instead of skipping the check.
     */
    public static Optional<String> findMinRequiredVersionForTradingOfAnyMaker(Collection<AuthorizedAlertData> alerts) {
        List<String> minVersions = MAKER_APP_TYPES.stream()
                .map(appType -> findMinRequiredVersionForTrading(alerts.stream(), appType))
                .flatMap(Optional::stream)
                .toList();
        Optional<String> notComparable = minVersions.stream()
                .filter(minVersion -> !isComparable(minVersion))
                .findFirst();
        if (notComparable.isPresent()) {
            log.warn("Min. version required for trading cannot be compared. minVersion={}", notComparable.get());
            return notComparable;
        }
        return minVersions.stream()
                .map(Version::new)
                .max(Comparator.naturalOrder())
                .map(Version::getVersionAsString);
    }

    private static boolean isComparable(String version) {
        try {
            new Version(version).compareTo(new Version("0"));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
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
                .filter(alert -> appliesTo(alert, appType))
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