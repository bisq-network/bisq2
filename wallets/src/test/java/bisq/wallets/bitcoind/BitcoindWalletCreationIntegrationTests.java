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

package bisq.wallets.bitcoind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoindWalletCreationIntegrationTests extends SharedBitcoindInstanceTests {
    @Test
    public void createFreshWallet() {
        assertTrue(walletFilePath.toFile().exists());
        assertEquals(0, minerWalletBackend.getBalance());
    }

    @Test
    public void loadWalletIfExisting() {
        assertTrue(walletFilePath.toFile().exists());

        minerChainBackend.unloadWallet(walletFilePath);
        minerChainBackend.createOrLoadWallet(walletFilePath, BitcoindRegtestSetup.WALLET_PASSPHRASE, false, false);

        assertEquals(0, minerWalletBackend.getBalance());
        assertTrue(walletFilePath.toFile().exists());
    }
}
