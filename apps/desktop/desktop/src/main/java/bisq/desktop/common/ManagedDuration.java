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

package bisq.desktop.common;

import javafx.util.Duration;

public class ManagedDuration {
    private static final int DEFAULT_DURATION = 600;
    private static final int SPLITPANE_ANIMATION_DURATION = 300;
    private static final int NOTIFICATION_PANEL_DURATION = DEFAULT_DURATION / 2;

    public static Duration ZERO = Duration.ZERO;

    public static Duration millis(long value) {
        return Transitions.useAnimations() ? Duration.millis(value) : Duration.ONE;
    }

    public static long inMillis(long millis) {
        return Transitions.useAnimations() ? millis : 1;
    }

    public static long toMillis(Duration duration) {
        return (long) duration.toMillis();
    }

    public static Duration getDefaultDuration() {
        return millis(DEFAULT_DURATION);
    }

    public static long getDefaultDurationMillis() {
        return toMillis(getDefaultDuration());
    }

    public static Duration getHalfOfDefaultDuration() {
        return millis(DEFAULT_DURATION / 2);
    }

    public static long getHalfOfDefaultDurationMillis() {
        return toMillis(getHalfOfDefaultDuration());
    }

    public static Duration getQuoterOfDefaultDuration() {
        return millis(DEFAULT_DURATION / 4);
    }

    public static long getQuoterOfDefaultDurationMillis() {
        return toMillis(getQuoterOfDefaultDuration());
    }

    public static Duration getOneSixthOfDefaultDuration() {
        return millis(DEFAULT_DURATION / 6);
    }

    public static long getOneSixthOfDefaultDurationMillis() {
        return toMillis(getOneSixthOfDefaultDuration());
    }

    public static Duration getOneEighthOfDefaultDuration() {
        return millis(DEFAULT_DURATION / 8);
    }

    public static long getOneEighthOfDefaultDurationMillis() {
        return toMillis(getOneEighthOfDefaultDuration());
    }

    public static Duration getSplitPaneAnimationDuration() {
        return millis(SPLITPANE_ANIMATION_DURATION);
    }

    public static long getSplitPaneAnimationDurationMillis() {
        return toMillis(getSplitPaneAnimationDuration());
    }

    public static Duration getNotificationPanelDuration() {
        return millis(NOTIFICATION_PANEL_DURATION);
    }

    public static long getNotificationPanelDurationMillis() {
        return toMillis(getNotificationPanelDuration());
    }
}
