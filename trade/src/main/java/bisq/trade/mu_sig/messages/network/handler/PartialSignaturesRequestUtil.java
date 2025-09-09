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

package bisq.trade.mu_sig.messages.network.handler;

import bisq.burningman.BurningmanData;
import bisq.trade.mu_sig.DelayedPayoutTxReceiverService;
import bisq.trade.protobuf.ReceiverAddressAndAmount;
import com.google.common.collect.ImmutableMap;

public class PartialSignaturesRequestUtil {
    // TODO We need to publish from the oracle data from bisq 1 to bisq 2 network so that the BM
    // addresses and receiver shares are accessible.
    // As this data is p2p network data we need some tolerance handling in case the traders have a different view of the
    // p2p network data.
    public static Iterable<ReceiverAddressAndAmount> getBurningMenDPTReceivers(long redirectionAmountMsat) {
        //noinspection SpellCheckingInspection
        var burningmanShares = ImmutableMap.of(
                        "bcrt1phc8m8vansnl4utths947mjquprw20puwrrdfrwx8akeeu2tqwklsnxsvf0", 0.6,
                        "bcrt1qwk6p86mzqmstcsg99qlu2mhsp3766u68jktv6k", 0.3,
                        "2N2x2bA28AsLZZEHss4SjFoyToQV5YYZsJM", 0.1
                )
                .entrySet().stream()
                .map(e -> new BurningmanData(e.getKey(), e.getValue()))
                .toList();
        long feeRate = NonceSharesRequestUtil.getPreparedTxFeeRate();
        return DelayedPayoutTxReceiverService.computeReceivers(burningmanShares, redirectionAmountMsat, feeRate);
    }
}
