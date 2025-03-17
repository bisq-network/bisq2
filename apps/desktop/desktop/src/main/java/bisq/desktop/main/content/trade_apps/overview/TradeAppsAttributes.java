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

package bisq.desktop.main.content.trade_apps.overview;

import lombok.Getter;

public class TradeAppsAttributes {
    @Getter
    public enum Type {
        BISQ_EASY(Security.LOW, Privacy.MID, Convenience.HIGH, Cost.HIGH, Speed.LOW),
        BISQ_MU_SIG(Security.MID, Privacy.MID, Convenience.MID, Cost.MID, Speed.LOW),
        SUBMARINE(Security.HIGH, Privacy.HIGH, Convenience.MID, Cost.LOW, Speed.HIGH),
        LIQUID_MU_SIG(Security.MID, Privacy.MID, Convenience.MID, Cost.LOW, Speed.LOW),
        BISQ_LIGHTNING(Security.MID, Privacy.MID, Convenience.MID, Cost.LOW, Speed.MID),
        LIQUID_SWAP(Security.HIGH, Privacy.HIGH, Convenience.MID, Cost.LOW, Speed.HIGH),
        BSQ_SWAP(Security.HIGH, Privacy.MID, Convenience.MID, Cost.MID, Speed.MID),
        LIGHTNING_ESCROW(Security.MID, Privacy.MID, Convenience.MID, Cost.LOW, Speed.MID),
        MONERO_SWAP(Security.HIGH, Privacy.HIGH, Convenience.LOW, Cost.MID, Speed.MID);

        private final Security security;
        private final Privacy privacy;
        private final Convenience convenience;
        private final Cost cost;
        private final Speed speed;

        Type(Security security, Privacy privacy, Convenience convenience, Cost cost, Speed speed) {
            this.security = security;
            this.privacy = privacy;
            this.convenience = convenience;
            this.cost = cost;
            this.speed = speed;
        }
    }

    public enum Security {
        LOW, MID, HIGH
    }

    public enum Privacy {
        LOW, MID, HIGH
    }

    public enum Convenience {
        LOW, MID, HIGH
    }

    public enum Cost {
        LOW, MID, HIGH
    }

    public enum Speed {
        LOW, MID, HIGH
    }
}

