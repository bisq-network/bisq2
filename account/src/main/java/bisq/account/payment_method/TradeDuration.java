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

package bisq.account.payment_method;

import bisq.i18n.Res;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

@Getter
public enum TradeDuration {
    HOURS_24(24, TimeUnit.HOURS),
    DAYS_2(2, TimeUnit.DAYS),
    DAYS_3(3, TimeUnit.DAYS),
    DAYS_4(4, TimeUnit.DAYS),
    DAYS_5(5, TimeUnit.DAYS),
    DAYS_6(6, TimeUnit.DAYS),
    DAYS_7(7, TimeUnit.DAYS),
    DAYS_8(8, TimeUnit.DAYS);

    private final long time;
    private final String displayString;

    TradeDuration(long duration, TimeUnit unit) {
        this.time = unit.toMillis(duration);
        if (unit == TimeUnit.HOURS) {
            this.displayString = Res.get("temporal.hour.*", (int) duration);
        } else {
            this.displayString = Res.get("temporal.day.*", (int) duration);
        }
    }
}
