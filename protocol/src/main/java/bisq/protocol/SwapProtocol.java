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

package bisq.protocol;

import lombok.Getter;

public class SwapProtocol {
    @Getter
    public enum Type {
        SATOSHI_SQUARE(Security.LOW, Privacy.LOW, Convenience.HIGH, Cost.HIGH, Speed.LOW),
        LIQUID_SWAP(Security.HIGH, Privacy.HIGH, Convenience.MID, Cost.LOW, Speed.HIGH),
        ATOMIC_CROSS_CHAIN_SWAP(Security.HIGH, Privacy.LOW, Convenience.MID, Cost.MID, Speed.MID),
        BISQ_MULTI_SIG(Security.MID, Privacy.LOW, Convenience.MID, Cost.MID, Speed.LOW),
        BSQ_SWAP(Security.HIGH, Privacy.MID, Convenience.MID, Cost.MID, Speed.MID),
        LN_3_PARTY(Security.MID, Privacy.MID, Convenience.MID, Cost.LOW, Speed.LOW);

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
        HIGH, MID, LOW
    }

    public enum Privacy {
        HIGH, MID, LOW
    }

    public enum Convenience {
        HIGH, MID, LOW
    }

    public enum Cost {
        HIGH, MID, LOW
    }

    public enum Speed {
        HIGH, MID, LOW
    }
}

