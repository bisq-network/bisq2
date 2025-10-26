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
import bisq.trade.protobuf.ReceiverAddressAndAmount;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static bisq.trade.mu_sig.DelayedPayoutTxReceiverService.scriptPubKeyLength;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DelayedPayoutTxReceiverServiceTest {
    @Test
    void testComputeReceivers_noneFiltered() {
        // These are the same available msat amount, fee rate & absolute receiver amounts used in the test trades of
        // 'TradeProtocolClient.java' from the bisq-musig project.
        long feeRateSatPerKwu = 10 * 250; // 10 sats per vbyte
        long availableAmountMsat = 256_115_000;

        // (The precise fractions chosen here sum to unity and exercise the use of 'Math::round' in 'computeReceivers'
        // to round scaled output amounts to nearest as intended, to minimize cumulative rounding errors prior to fixup.
        // Casting directly to long rounds _down_ instead, leading to slightly different amounts which fail the test.)
        //noinspection SpellCheckingInspection
        var burningmanDataList = burningmanDataList(ImmutableMap.of(
                "2N2x2bA28AsLZZEHss4SjFoyToQV5YYZsJM", 0.059_026,
                "bcrt1qwk6p86mzqmstcsg99qlu2mhsp3766u68jktv6k", 0.313_659,
                "bcrt1phc8m8vansnl4utths947mjquprw20puwrrdfrwx8akeeu2tqwklsnxsvf0", 0.627_315
        ).entrySet());

        //noinspection SpellCheckingInspection
        var expectedReceivers = receivers(ImmutableMap.of(
                "bcrt1phc8m8vansnl4utths947mjquprw20puwrrdfrwx8akeeu2tqwklsnxsvf0", 160_000,
                "bcrt1qwk6p86mzqmstcsg99qlu2mhsp3766u68jktv6k", 80_000,
                "2N2x2bA28AsLZZEHss4SjFoyToQV5YYZsJM", 15_055
        ).entrySet());

        var receivers = DelayedPayoutTxReceiverService.computeReceivers(
                burningmanDataList, availableAmountMsat, feeRateSatPerKwu);

        assertEquals(expectedReceivers, receivers);
    }

    @Test
    void testComputeReceivers_oneFilteredNormally() {
        // This is the smallest fee for which the minimum allowed receiver output amount starts scaling linearly with
        // the fee and exceeds the absolute minimum of 1000 sats, instead being rounded _up_ to 1001 sats:
        long feeRateSatPerKwu = 3_907; // 15.628 sats per vbyte
        // 2000 sats for two P2TR outputs, plus 1_344_008 msat for their fee contributions:
        long availableAmountMsat = 3_344_008;

        // To be paid 1000 sats each, if both were included:
        //noinspection SpellCheckingInspection
        var burningmanDataList = burningmanDataList(ImmutableMap.of(
                "bcrt1p88h9s6lq8jw3ehdlljp7sa85kwpp9lvyrl077twvjnackk4lxt0sffnlrk", 0.5,
                "bcrt1phhl8d90r9haqwtvw2cv4ryjl8tlnqrv48nhpy7yyks5du6mr66xq5nlwhz", 0.5
        ).entrySet());

        // The second receiver gets filtered out, falling short of the minimum output amount by 1 sat. The first
        // receiver is not filtered out, in spite of having the same share, since removing the remaining receivers makes
        // another 1_672 sats available, taking him above the minimum.
        //noinspection SpellCheckingInspection
        var expectedReceivers = receivers(ImmutableMap.of(
                "bcrt1p88h9s6lq8jw3ehdlljp7sa85kwpp9lvyrl077twvjnackk4lxt0sffnlrk", 2_672
        ).entrySet());

        var receivers = DelayedPayoutTxReceiverService.computeReceivers(
                burningmanDataList, availableAmountMsat, feeRateSatPerKwu);

        assertEquals(expectedReceivers, receivers);
    }

    @Test
    void testComputeReceivers_oneFilteredByMinOutputSaturation() {
        long feeRateSatPerKwu = 10 * 250; // 10 sats per vbyte
        // 1999 sats for two P2TR outputs, plus 860 sats for their fee contributions:
        long availableAmountMsat = 2_859_000;

        // To be paid 1000 sats each, if both were included, since the 999.5 sats owed to each receiver gets rounded up
        // to 1000. But since this would lead to a total overpayment of 1 sat, and 1000 is the minimum output amount, we
        // are not allowed to include both.
        //noinspection SpellCheckingInspection
        var burningmanDataList = burningmanDataList(ImmutableMap.of(
                "bcrt1p88h9s6lq8jw3ehdlljp7sa85kwpp9lvyrl077twvjnackk4lxt0sffnlrk", 0.5,
                "bcrt1phhl8d90r9haqwtvw2cv4ryjl8tlnqrv48nhpy7yyks5du6mr66xq5nlwhz", 0.5
        ).entrySet());

        // The second receiver gets filtered out, in spite of just reaching the 1000 sat minimum needed to be included,
        // since 'minOutputAmount' * 2 exceeds the 1999 sats available for two P2TR outputs. The first receiver is not
        // filtered out, in spite of having the same share, since 'minOutputAmount' * 1 is less than the 2_429 sats
        // available for one P2TR output.
        //noinspection SpellCheckingInspection
        var expectedReceivers = receivers(ImmutableMap.of(
                "bcrt1p88h9s6lq8jw3ehdlljp7sa85kwpp9lvyrl077twvjnackk4lxt0sffnlrk", 2_429
        ).entrySet());

        var receivers = DelayedPayoutTxReceiverService.computeReceivers(
                burningmanDataList, availableAmountMsat, feeRateSatPerKwu);

        assertEquals(expectedReceivers, receivers);
    }

    @Test
    void testComputeReceivers_allFiltered() {
        long feeRateSatPerKwu = 10 * 250; // 10 sats per vbyte
        // 999 sats for one P2TR output, plus 430 sats for its fee contribution:
        long availableAmountMsat = 1_149_008;

        // The first receiver to be paid 999 sats, if he alone was included:
        //noinspection SpellCheckingInspection
        var burningmanDataList = burningmanDataList(ImmutableMap.of(
                "bcrt1p88h9s6lq8jw3ehdlljp7sa85kwpp9lvyrl077twvjnackk4lxt0sffnlrk", 0.5,
                "bcrt1phhl8d90r9haqwtvw2cv4ryjl8tlnqrv48nhpy7yyks5du6mr66xq5nlwhz", 0.5
        ).entrySet());

        // Computing the receiver list fails, as none could be economically included.
        assertThrows(IllegalArgumentException.class, () ->
                DelayedPayoutTxReceiverService.computeReceivers(
                        burningmanDataList, availableAmountMsat, feeRateSatPerKwu));
    }

    @Test
    void testComputeReceivers_moreThan251Outputs() {
        long feeRateSatPerKwu = 250; // 1 sat per vbyte
        // 10_000 sats each for 252 P2SH outputs, 8_064 = 252 * 32 sats for their fee contributions, but this _doesn't_
        // include the extra 2 vB <-> 2 sats cost for including >251 outputs:
        long availableAmountMsat = 2_528_064_000L;

        // Receiver address repetitions shouldn't normally occur but are permitted, as well as shares that don't sum to
        // unity (which would be difficult to achieve precisely due to rounding errors anyway). Instead, the shares are
        // implicitly divided by their total and need only be non-negative and finite.
        //noinspection SpellCheckingInspection
        var burningmanDataList = burningmanDataList(ImmutableListMultimap.<String, Double>builder().putAll(
                "2N2x2bA28AsLZZEHss4SjFoyToQV5YYZsJM", Collections.nCopies(252, 1.0)
        ).build().entries());

        // Since the available amount is short by 2 sats, the last two output amounts are 9_999 sats each instead of the
        // 10_000 sats for each of the first 250 outputs.
        //noinspection SpellCheckingInspection
        var expectedReceivers = receivers(ImmutableListMultimap.<String, Integer>builder().putAll(
                "2N2x2bA28AsLZZEHss4SjFoyToQV5YYZsJM", Collections.nCopies(250, 10_000)
        ).putAll(
                "2N2x2bA28AsLZZEHss4SjFoyToQV5YYZsJM", Collections.nCopies(2, 9_999)
        ).build().entries());

        var receivers = DelayedPayoutTxReceiverService.computeReceivers(
                burningmanDataList, availableAmountMsat, feeRateSatPerKwu);

        assertEquals(expectedReceivers, receivers);

        // Now add an extra 10_032 sats to the available total and a 253rd receiver share identical to the others. This
        // means we are still short by 2 sats, so only the last two outputs are 9_999 sats, as before. (That is, the 2
        // vB size cost should only be incurred once.)
        availableAmountMsat = availableAmountMsat / 252 * 253;
        burningmanDataList.add(burningmanDataList.getFirst());
        expectedReceivers.add(250, expectedReceivers.getFirst());

        receivers = DelayedPayoutTxReceiverService.computeReceivers(
                burningmanDataList, availableAmountMsat, feeRateSatPerKwu);

        assertEquals(expectedReceivers, receivers);
    }

    private static List<BurningmanData> burningmanDataList(Collection<Map.Entry<String, Double>> entries) {
        return entries.stream()
                .map(e -> new BurningmanData(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<ReceiverAddressAndAmount> receivers(Collection<Map.Entry<String, Integer>> entries) {
        return entries.stream()
                .map(e -> ReceiverAddressAndAmount.newBuilder()
                        .setAddress(e.getKey())
                        .setAmount(e.getValue())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testScriptPubKeyLength() {
        assertEquals(25, scriptPubKeyLength("11111111111111111111BZbvjr")); // mainnet P2PKH
        assertEquals(25, scriptPubKeyLength("1BgGZ9tcN4rm9KBzDn7KprQz87SZ26SAMH")); // mainnet P2PKH
        assertEquals(25, scriptPubKeyLength("mrCDrCybB6J1vRfbwM5hemdJz73FwDBC8r")); // testnet/regtest P2PKH
        assertEquals(25, scriptPubKeyLength("n42m3hGC52QTChUbXq3QAPVU6nWkG9xuWj")); // testnet/regtest P2PKH
        assertEquals(23, scriptPubKeyLength("3LRW7jeCvQCRdPF8S3yUCfRAx4eqXFmdcr")); // mainnet P2SH
        assertEquals(23, scriptPubKeyLength("2NByiBUaEXrhmqAsg7BbLpcQSAQs1EDwt5w")); // testnet/regtest P2SH
        assertEquals(22, scriptPubKeyLength("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4")); // mainnet P2WPKH
        assertEquals(22, scriptPubKeyLength("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")); // mainnet P2WPKH
        assertEquals(22, scriptPubKeyLength("tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx")); // testnet P2WPKH
        assertEquals(22, scriptPubKeyLength("bcrt1qwk6p86mzqmstcsg99qlu2mhsp3766u68jktv6k")); // regtest P2WPKH
        assertEquals(34, scriptPubKeyLength("bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3")); // mainnet P2WSH
        assertEquals(34, scriptPubKeyLength("tb1qqqqqp399et2xygdj5xreqhjjvcmzhxw4aywxecjdzew6hylgvsesrxh6hy")); // testnet P2WSH
        assertEquals(34, scriptPubKeyLength("bcrt1qna5vangp5vpvjulxjtjncreygkrj2jt2dar5pvq8tn0qt0wqj60s83aga7")); // regtest P2WSH
        assertEquals(34, scriptPubKeyLength("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0")); // mainnet P2TR
        assertEquals(34, scriptPubKeyLength("tb1pqqqqp399et2xygdj5xreqhjjvcmzhxw4aywxecjdzew6hylgvsesf3hn0c")); // testnet P2TR
        assertEquals(34, scriptPubKeyLength("bcrt1phc8m8vansnl4utths947mjquprw20puwrrdfrwx8akeeu2tqwklsnxsvf0")); // regtest P2TR
        assertEquals(4, scriptPubKeyLength("bc1pfeessrawgf")); // mainnet P2A
        assertEquals(4, scriptPubKeyLength("tb1pfees9rn5nz")); // testnet P2A
        assertEquals(4, scriptPubKeyLength("bcrt1pfeesnyr2tx")); // regtest P2A
    }
}
