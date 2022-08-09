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

package bisq.user.reputation;

import bisq.i18n.Res;
import lombok.Getter;

public enum ReputationSource {
    BURNED_BSQ(Res.get("reputation.source.BURNED_BSQ")),
    BSQ_BOND(Res.get("reputation.source.BSQ_BOND")),
    PROFILE_AGE(Res.get("reputation.source.PROFILE_AGE")),
    BISQ1_ACCOUNT_AGE(Res.get("reputation.source.BISQ1_ACCOUNT_AGE")),
    BISQ1_SIGNED_ACCOUNT_AGE_WITNESS(Res.get("reputation.source.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS"));

    @Getter
    private final String displayString;

    ReputationSource(String displayString) {
        this.displayString = displayString;
    }
}