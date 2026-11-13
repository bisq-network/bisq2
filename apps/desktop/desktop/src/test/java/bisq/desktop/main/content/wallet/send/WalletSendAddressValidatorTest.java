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

package bisq.desktop.main.content.wallet.send;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletSendAddressValidatorTest {
    private static final String MAINNET_BECH32 = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq";
    private static final String MAINNET_BASE58 = "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2";
    private static final String REGTEST_BECH32 = "bcrt1qw508d6qejxtdg4y5r3zarvary0c5xw7kygt080";
    private static final String TESTNET_BECH32 = "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx";
    private static final String TESTNET_BASE58 = "mipcBbFg9gMiCh81Kj8tqqdgoZub1ZJRfn";

    @Test
    void mainnetAddressesAreValidOnBothNetworks() {
        for (boolean devMode : new boolean[]{false, true}) {
            assertEquals(MAINNET_BECH32, WalletSendAddressValidator.canonical(MAINNET_BECH32, devMode).orElseThrow());
            assertEquals(MAINNET_BASE58, WalletSendAddressValidator.canonical(MAINNET_BASE58, devMode).orElseThrow());
        }
    }

    @Test
    void testNetworkAddressesAreValidOnlyInDevMode() {
        for (String address : new String[]{REGTEST_BECH32, TESTNET_BECH32, TESTNET_BASE58}) {
            assertEquals(address, WalletSendAddressValidator.canonical(address, true).orElseThrow());
            assertTrue(WalletSendAddressValidator.canonical(address, false).isEmpty(), address);
        }
    }

    @Test
    void theAddressIsSentAsTheDaemonExpectsIt() {
        assertEquals(MAINNET_BECH32, WalletSendAddressValidator.canonical("  " + MAINNET_BECH32 + "\n", false).orElseThrow());
        assertEquals(MAINNET_BECH32, WalletSendAddressValidator.canonical("bitcoin:" + MAINNET_BECH32 + "?amount=0.5", false).orElseThrow());
        assertEquals(MAINNET_BECH32, WalletSendAddressValidator.canonical(MAINNET_BECH32.toUpperCase(), false).orElseThrow());
        assertEquals(REGTEST_BECH32, WalletSendAddressValidator.canonical(REGTEST_BECH32.toUpperCase(), true).orElseThrow());
        assertEquals(MAINNET_BASE58, WalletSendAddressValidator.canonical("BITCOIN:" + MAINNET_BASE58, false).orElseThrow());
    }

    // BIP 21: a client must not act on a URI with a req- parameter which it does not support
    @Test
    void aPaymentUriWithARequiredParameterIsInvalid() {
        for (String query : new String[]{"?req-pop=abc", "?amount=0.5&req-future=1", "?REQ-pop=abc", "?req-flag"}) {
            assertTrue(WalletSendAddressValidator.canonical("bitcoin:" + MAINNET_BECH32 + query, false).isEmpty(), query);
        }
    }

    @Test
    void otherUriParametersAreDropped() {
        for (String query : new String[]{"?amount=0.5&label=shop", "?label=req-1", "?prereq-x=1"}) {
            assertEquals(MAINNET_BECH32,
                    WalletSendAddressValidator.canonical("bitcoin:" + MAINNET_BECH32 + query, false).orElseThrow(), query);
        }
    }

    @Test
    void emptyAndMalformedInputIsInvalid() {
        for (boolean devMode : new boolean[]{false, true}) {
            for (String input : new String[]{null, "", "   ", "bc1", "bc1-not-an-address", "0x52908400098527886E0F7030069857D2E4169EE7"}) {
                assertTrue(WalletSendAddressValidator.canonical(input, devMode).isEmpty(), String.valueOf(input));
            }
        }
    }
}
