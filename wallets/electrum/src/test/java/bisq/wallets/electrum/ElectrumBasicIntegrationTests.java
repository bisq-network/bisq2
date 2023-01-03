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

package bisq.wallets.electrum;

import bisq.wallets.electrum.regtest.ElectrumExtension;
import bisq.wallets.electrum.regtest.electrum.ElectrumRegtestSetup;
import bisq.wallets.electrum.regtest.electrum.MacLinuxElectrumRegtestSetup;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ElectrumExtension.class)
public class ElectrumBasicIntegrationTests {
    private final ElectrumRegtestSetup electrumRegtestSetup;
    private ElectrumDaemon electrumDaemon;

    public ElectrumBasicIntegrationTests(ElectrumRegtestSetup electrumRegtestSetup) {
        this.electrumRegtestSetup = electrumRegtestSetup;
    }

    @BeforeEach
    void setUp() {
        electrumDaemon = electrumRegtestSetup.getElectrumDaemon();
    }

    @Test
    void getBalanceTest() {
        double balance = electrumDaemon.getBalance();
        assertThat(balance).isGreaterThanOrEqualTo(0);
    }

    @Test
    void getSeedTest() {
        String seed = electrumDaemon.getSeed(Optional.of(MacLinuxElectrumRegtestSetup.WALLET_PASSPHRASE));
        String expectedSeed = electrumRegtestSetup.getWalletInfo().getResult().getSeed();
        assertThat(seed).isEqualTo(expectedSeed);
    }

    @Test
    void getUnusedAddressTest() {
        String address = electrumDaemon.getUnusedAddress();
        assertThat(address).startsWith("bcr");
    }

    @Test
    void signAndVerifyMessageTest() {
        String address = electrumDaemon.getUnusedAddress();
        String message = "My proof that I own " + address;
        String signature = electrumDaemon.signMessage(Optional.of(MacLinuxElectrumRegtestSetup.WALLET_PASSPHRASE), address, message);

        boolean isValid = electrumDaemon.verifyMessage(address, signature, message);
        assertThat(isValid).isTrue();
    }
}
