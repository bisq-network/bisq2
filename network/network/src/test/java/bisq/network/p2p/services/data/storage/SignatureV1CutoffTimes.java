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

import bisq.common.timer.Clock;

/**
 * Times of a node around SignedRequest.SIGNATURE_V1_CUTOFF_DATE, for tests with requests without signature V2. A test
 * which uses one of these times does not depend on the date at which it runs.
 */
public final class SignatureV1CutoffTimes {
    // The last time at which a node accepts requests without signature V2
    public static final long BEFORE_CUTOFF = SignedRequest.SIGNATURE_V1_CUTOFF_DATE.getTime() - 1;
    // The first time at which a node requires signature V2
    public static final long AT_CUTOFF = SignedRequest.SIGNATURE_V1_CUTOFF_DATE.getTime();

    public static final Clock CLOCK_BEFORE_CUTOFF = () -> BEFORE_CUTOFF;
    public static final Clock CLOCK_AT_CUTOFF = () -> AT_CUTOFF;

    private SignatureV1CutoffTimes() {
    }
}
