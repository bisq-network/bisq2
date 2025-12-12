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

import bisq.burningman.BurningmanData;
import bisq.burningman.BurningmanService;
import bisq.common.application.Service;
import bisq.trade.protobuf.ReceiverAddressAndAmount;
import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class DelayedPayoutTxReceiverService implements Service {
    // Outputs paying less than this absolute satoshi amount are excluded:
    private static final long MIN_OUTPUT_AMOUNT = 1000;
    // Twice the cost (32 bytes) of a P2SH output -- outputs paying less than this weight-equivalent are excluded:
    private static final long MIN_OUTPUT_EQUIVALENT_WEIGHT = 256;

    private static final Pattern P2SH_PATTERN = Pattern.compile("^[23][1-9A-HJ-NP-Za-km-z]{33,34}$");
    private static final Pattern P2PKH_PATTERN = Pattern.compile("^[1mn][1-9A-HJ-NP-Za-km-z]{25,33}$");
    private static final Pattern BECH32_PATTERN = Pattern.compile("^(?i)(?:bc|tb|bcrt)1([02-9ac-hj-np-z]{11,71})$");

    private final BurningmanService burningmanService;

    public DelayedPayoutTxReceiverService(BurningmanService burningmanService) {
        this.burningmanService = burningmanService;
    }

    public static List<ReceiverAddressAndAmount> computeReceivers(List<BurningmanData> burningmanDataList,
                                                                  long availableAmountMsat,
                                                                  long feeRateSatPerKwu) {
        // Sort the receivers by order of decreasing share size.
        var sortedBM = new ArrayList<>(burningmanDataList);
        sortedBM.sort(Comparator.comparingDouble(BurningmanData::getCappedBurnAmountShare).reversed());

        // Compute the total to receive after subtracting the tx fee, and which receivers can be economically included.
        long minOutputAmount = minOutputAmount(feeRateSatPerKwu);
        long totalToReceiveMsat = availableAmountMsat;
        double totalShare = 0.0;
        int numReceivers = 0;
        for (var bm : sortedBM) {
            double newTotalShare = totalShare + bm.getCappedBurnAmountShare();
            long newTotalToReceiveMsat = totalToReceiveMsat - outputCostMsat(bm.getReceiverAddress(), numReceivers, feeRateSatPerKwu);
            long newTotalToReceive = newTotalToReceiveMsat / 1000;
            long outputAmount = Math.round(newTotalToReceive * (bm.getCappedBurnAmountShare() / newTotalShare));
            if (outputAmount < minOutputAmount) {
                // No further receivers with this share size or smaller can be economically added.
                break;
            }
            if (minOutputAmount * (numReceivers + 1) > newTotalToReceive) {
                // We would get stuck if we include any more outputs, since there are insufficient funds for all of
                // them, even paying each the allowed minimum.
                break;
            }
            totalShare = newTotalShare;
            totalToReceiveMsat = newTotalToReceiveMsat;
            numReceivers++;
        }
        if (numReceivers == 0) {
            throw new IllegalArgumentException("Could not economically include any receivers: " +
                    "availableAmountMsat=" + availableAmountMsat + ", feeRateSatPerKwu=" + feeRateSatPerKwu);
        }

        // Convert the included shares into absolute satoshi amounts, subject to rounding errors.
        double finalTotalShare = totalShare;
        long totalToReceive = totalToReceiveMsat / 1000;
        long[] outputAmounts = sortedBM.stream()
                .limit(numReceivers)
                .mapToLong(bm -> Math.round(totalToReceive * (bm.getCappedBurnAmountShare() / finalTotalShare)))
                .toArray();

        // Correct any slight total overpayment/underpayment to the receivers (likely just a few satoshis), by
        // subtracting/adding a uniform (or as close to uniform as possible) amount from/to each output, favoring the
        // bigger receivers and making sure that no receiver amount dips below the fee dependent 'minOutputAmount'.
        long underpayment = totalToReceive - Arrays.stream(outputAmounts).sum();
        for (int i = numReceivers; i-- > 0; ) {
            long correction = Math.max(Math.floorDiv(underpayment, i + 1), minOutputAmount - outputAmounts[i]);
            outputAmounts[i] += correction;
            underpayment -= correction;
        }
        return IntStream.range(0, numReceivers)
                .mapToObj(i -> ReceiverAddressAndAmount.newBuilder()
                        .setAddress(sortedBM.get(i).getReceiverAddress())
                        .setAmount(outputAmounts[i])
                        .build())
                .toList();
    }

    private static long minOutputAmount(long feeRateSatPerKwu) {
        return Math.max(MIN_OUTPUT_AMOUNT, (MIN_OUTPUT_EQUIVALENT_WEIGHT * feeRateSatPerKwu + 999) / 1000);
    }

    private static long outputCostMsat(String address, int index, long feeRateSatPerKwu) {
        int weight = 4 * (scriptPubKeyLength(address) + (index == 251 ? 11 : 9));
        return weight * feeRateSatPerKwu;
    }

    @VisibleForTesting
    static int scriptPubKeyLength(String address) {
        var matcher = BECH32_PATTERN.matcher(address);
        if (matcher.matches()) {
            return matcher.group(1).length() * 5 / 8 - 2;
        }
        if (P2SH_PATTERN.matcher(address).matches()) {
            return 23;
        }
        if (P2PKH_PATTERN.matcher(address).matches()) {
            return 25;
        }
        throw new IllegalArgumentException("Unrecognized address type: " + address);
    }
}
