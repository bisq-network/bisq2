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

package bisq.trade.mu_sig;

import static com.google.common.base.Preconditions.checkArgument;

// TODO Add fee estimation input to determine fee rate
public class MusSigFeeRateProvider {
    public static final long DEFAULT_OPTIMAL_SAT_PER_V_BYTE = 10;

    private static long optimalSatPerVByte = DEFAULT_OPTIMAL_SAT_PER_V_BYTE;

    public static void setOptimalSatPerVByte(long optimalSatPerVByte) {
        checkArgument(optimalSatPerVByte >= 1);
        checkArgument(optimalSatPerVByte <= 1000); // Just some sanity check. Maybe we let user set max fee rate in settings.
        MusSigFeeRateProvider.optimalSatPerVByte = optimalSatPerVByte;
    }

    public static long getDepositTxFeeRate() {
        return 5000 * optimalSatPerVByte;
    }

    public static long getPreparedTxFeeRate() {
        return 4000 * optimalSatPerVByte;
    }
}
