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

import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.WalletStartupTests;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import bisq.wallets.regtest.process.MultiProcessCoordinator;

import java.io.IOException;

public class BitcoindStartupIntegrationTests extends WalletStartupTests<MultiProcessCoordinator, BitcoindWallet> {
    @Override
    public AbstractRegtestSetup<MultiProcessCoordinator> createRegtestSetup() throws IOException {
        return new BitcoindRegtestSetup();
    }
}
