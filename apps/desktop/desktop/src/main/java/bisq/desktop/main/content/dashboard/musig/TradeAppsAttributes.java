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

package bisq.desktop.main.content.dashboard.musig;

import lombok.Getter;

public class TradeAppsAttributes {
    @Getter
    public enum Type {
        BISQ_EASY(Security.LOW, Privacy.MID, EaseOfUse.HIGH),
        MU_SIG(Security.HIGH, Privacy.MID, EaseOfUse.LOW);

        private final Security security;
        private final Privacy privacy;
        private final EaseOfUse convenience;

        Type(Security security, Privacy privacy, EaseOfUse convenience) {
            this.security = security;
            this.privacy = privacy;
            this.convenience = convenience;
        }
    }

    public enum Security {
        LOW, MID, HIGH
    }

    public enum Privacy {
        LOW, MID, HIGH
    }

    public enum EaseOfUse {
        LOW, MID, HIGH
    }
}
