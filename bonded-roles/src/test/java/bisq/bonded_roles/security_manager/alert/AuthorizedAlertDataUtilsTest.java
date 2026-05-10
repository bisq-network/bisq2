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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizedAlertDataUtilsTest {

    // Base time shared across all tests so the relative ordering encoded in the offset values
    // is preserved while all constructed timestamps remain recent enough to pass NetworkDataValidation.
    private static final long NOW = System.currentTimeMillis();

    @Test
    @DisplayName("find most recent trade restricting alert returns empty when no alerts")
    void find_most_recent_trade_restricting_alert_returns_empty_when_no_alerts() {
        Optional<AuthorizedAlertData> result = AuthorizedAlertDataUtils.findMostRecentTradeRestrictingAlert(
                Stream.empty(), AppType.DESKTOP);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("find most recent trade restricting alert filters non trade restricting alerts")
    void find_most_recent_trade_restricting_alert_filters_non_trade_restricting_alerts() {
        // INFO alerts are not trade-restricting
        AuthorizedAlertData infoAlert = createAlert("info-1", AppType.DESKTOP, 10, AlertType.INFO, false, false);

        Optional<AuthorizedAlertData> result = AuthorizedAlertDataUtils.findMostRecentTradeRestrictingAlert(
                Stream.of(infoAlert), AppType.DESKTOP);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("find most recent trade restricting alert filters alerts for other app types")
    void find_most_recent_trade_restricting_alert_filters_alerts_for_other_app_types() {
        AuthorizedAlertData mobileHalt = createAlert("mobile-halt", AppType.MOBILE_CLIENT, 10, AlertType.EMERGENCY, true, false);

        Optional<AuthorizedAlertData> result = AuthorizedAlertDataUtils.findMostRecentTradeRestrictingAlert(
                Stream.of(mobileHalt), AppType.DESKTOP);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("find most recent trade restricting alert returns single halt trading alert")
    void find_most_recent_trade_restricting_alert_returns_single_halt_trading_alert() {
        AuthorizedAlertData haltAlert = createAlert("halt-1", AppType.DESKTOP, 10, AlertType.EMERGENCY, true, false);

        Optional<AuthorizedAlertData> result = AuthorizedAlertDataUtils.findMostRecentTradeRestrictingAlert(
                Stream.of(haltAlert), AppType.DESKTOP);

        assertThat(result).contains(haltAlert);
    }

    @Test
    @DisplayName("find most recent trade restricting alert returns single require version alert")
    void find_most_recent_trade_restricting_alert_returns_single_require_version_alert() {
        AuthorizedAlertData requireVersion = createAlert("req-1", AppType.DESKTOP, 10, AlertType.EMERGENCY, false, true, Optional.of("2.1.0"));

        Optional<AuthorizedAlertData> result = AuthorizedAlertDataUtils.findMostRecentTradeRestrictingAlert(
                Stream.of(requireVersion), AppType.DESKTOP);

        assertThat(result).contains(requireVersion);
    }

    @Test
    @DisplayName("find most recent trade restricting alert returns only most recent halt trading alert")
    void find_most_recent_trade_restricting_alert_returns_only_most_recent_halt_trading_alert() {
        AuthorizedAlertData haltOld = createAlert("halt-old", AppType.DESKTOP, 10, AlertType.EMERGENCY, true, false);
        AuthorizedAlertData haltNew = createAlert("halt-new", AppType.DESKTOP, 30, AlertType.EMERGENCY, true, false);
        AuthorizedAlertData haltMid = createAlert("halt-mid", AppType.DESKTOP, 20, AlertType.EMERGENCY, true, false);

        Optional<AuthorizedAlertData> result = AuthorizedAlertDataUtils.findMostRecentTradeRestrictingAlert(
                Stream.of(haltOld, haltNew, haltMid), AppType.DESKTOP);

        assertThat(result).contains(haltNew);
    }

    @Test
    @DisplayName("find most recent trade restricting alert returns only most recent require version alert")
    void find_most_recent_trade_restricting_alert_returns_only_most_recent_require_version_alert() {
        AuthorizedAlertData reqOld = createAlert("req-old", AppType.DESKTOP, 10, AlertType.EMERGENCY, false, true, Optional.of("2.0.0"));
        AuthorizedAlertData reqNew = createAlert("req-new", AppType.DESKTOP, 60, AlertType.EMERGENCY, false, true, Optional.of("2.1.5"));
        AuthorizedAlertData reqMid = createAlert("req-mid", AppType.DESKTOP, 40, AlertType.EMERGENCY, false, true, Optional.of("2.1.3"));

        Optional<AuthorizedAlertData> result = AuthorizedAlertDataUtils.findMostRecentTradeRestrictingAlert(
                Stream.of(reqOld, reqNew, reqMid), AppType.DESKTOP);

        assertThat(result).contains(reqNew);
    }

    @Test
    @DisplayName("find most recent trade restricting alert prefers halt trading over require version even when older")
    void find_most_recent_trade_restricting_alert_prefers_halt_trading_over_require_version_even_when_older() {
        // haltTrading alert is older (offset 10) but still takes precedence over requireVersion (offset 20)
        AuthorizedAlertData haltAlert = createAlert("halt-1", AppType.DESKTOP, 10, AlertType.EMERGENCY, true, false);
        AuthorizedAlertData requireVersion = createAlert("req-1", AppType.DESKTOP, 20, AlertType.EMERGENCY, false, true, Optional.of("2.1.0"));

        Optional<AuthorizedAlertData> result = AuthorizedAlertDataUtils.findMostRecentTradeRestrictingAlert(
                Stream.of(requireVersion, haltAlert), AppType.DESKTOP);

        assertThat(result).hasValueSatisfying(alert -> assertThat(alert.getId()).isEqualTo("halt-1"));
    }

    @Test
    @DisplayName("find most recent trade restricting alert picks most recent halt from mixed input")
    void find_most_recent_trade_restricting_alert_picks_most_recent_halt_from_mixed_input() {
        AuthorizedAlertData haltOld = createAlert("halt-old", AppType.MOBILE_CLIENT, 10, AlertType.EMERGENCY, true, false);
        AuthorizedAlertData haltNew = createAlert("halt-new", AppType.MOBILE_CLIENT, 30, AlertType.EMERGENCY, true, false);
        AuthorizedAlertData reqOld = createAlert("req-old", AppType.MOBILE_CLIENT, 40, AlertType.EMERGENCY, false, true, Optional.of("2.1.3"));
        AuthorizedAlertData reqNew = createAlert("req-new", AppType.MOBILE_CLIENT, 60, AlertType.EMERGENCY, false, true, Optional.of("2.1.5"));
        AuthorizedAlertData desktopAlert = createAlert("desktop-halt", AppType.DESKTOP, 100, AlertType.EMERGENCY, true, false);

        Optional<AuthorizedAlertData> result = AuthorizedAlertDataUtils.findMostRecentTradeRestrictingAlert(
                Stream.of(haltOld, reqOld, haltNew, reqNew, desktopAlert), AppType.MOBILE_CLIENT);

        // halt takes precedence; most recent halt is halt-new
        assertThat(result).hasValueSatisfying(alert -> assertThat(alert.getId()).isEqualTo("halt-new"));
    }

    @Test
    @DisplayName("find most recent trade restricting alert returns require version when no halt present")
    void find_most_recent_trade_restricting_alert_returns_require_version_when_no_halt_present() {
        AuthorizedAlertData reqOld = createAlert("req-old", AppType.MOBILE_CLIENT, 10, AlertType.EMERGENCY, false, true, Optional.of("2.1.3"));
        AuthorizedAlertData reqNew = createAlert("req-new", AppType.MOBILE_CLIENT, 60, AlertType.EMERGENCY, false, true, Optional.of("2.1.5"));
        AuthorizedAlertData desktopAlert = createAlert("desktop-halt", AppType.DESKTOP, 100, AlertType.EMERGENCY, true, false);

        Optional<AuthorizedAlertData> result = AuthorizedAlertDataUtils.findMostRecentTradeRestrictingAlert(
                Stream.of(reqOld, reqNew, desktopAlert), AppType.MOBILE_CLIENT);

        assertThat(result).hasValueSatisfying(alert -> assertThat(alert.getId()).isEqualTo("req-new"));
    }

    // ---- helpers ----

    private AuthorizedAlertData createAlert(String id, AppType appType, int relativeOffset, AlertType alertType,
                                            boolean haltTrading, boolean requireVersionForTrading) {
        return createAlert(id, appType, relativeOffset, alertType, haltTrading, requireVersionForTrading, Optional.empty());
    }

    /**
     * Creates an alert with a timestamp of {@code NOW + relativeOffset}, making all timestamps
     * recent enough to pass {@link bisq.common.validation.NetworkDataValidation#validateDate}.
     * {@code relativeOffset} encodes relative ordering: a higher value means a more-recent alert.
     */
    private AuthorizedAlertData createAlert(String id, AppType appType, int relativeOffset, AlertType alertType,
                                            boolean haltTrading, boolean requireVersionForTrading,
                                            Optional<String> minVersion) {
        return new AuthorizedAlertData(
                id,
                NOW + relativeOffset,
                alertType,
                Optional.of("Headline " + id),
                Optional.of("Message " + id),
                haltTrading,
                requireVersionForTrading,
                minVersion,
                Optional.empty(),
                "0123456789abcdef0123456789abcdef01234567",
                false,
                Optional.empty(),
                appType
        );
    }
}
