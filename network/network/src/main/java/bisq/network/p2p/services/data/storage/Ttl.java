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

package bisq.network.p2p.services.data.storage;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * How long data of a given type is kept in the distributed storage. The set is closed on purpose: the ttl is
 * protocol level policy shared by all nodes, so adding a value is a deliberate decision, not a local choice.
 */
@Getter
public enum Ttl {
    MINUTES_10(TimeUnit.MINUTES.toMillis(10)),
    DAYS_2(TimeUnit.DAYS.toMillis(2)),
    DAYS_5(TimeUnit.DAYS.toMillis(5)),
    DAYS_10(TimeUnit.DAYS.toMillis(10)),
    DAYS_15(TimeUnit.DAYS.toMillis(15)),
    DAYS_20(TimeUnit.DAYS.toMillis(20)),
    DAYS_30(TimeUnit.DAYS.toMillis(30)),
    DAYS_100(TimeUnit.DAYS.toMillis(100));

    private final long millis;

    Ttl(long millis) {
        this.millis = millis;
    }
}
