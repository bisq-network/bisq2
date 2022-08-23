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

import bisq.wallets.electrum.regtest.electrum.ElectrumRegtestSetup;
import bisq.wallets.electrum.regtest.electrum.MacLinuxElectrumRegtestSetup;
import bisq.wallets.electrum.regtest.electrum.WindowsElectrumRegtestSetup;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.electrum.rpc.responses.ElectrumCreateResponse;
import bisq.wallets.electrum.rpc.responses.ElectrumGetInfoResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElectrumSetupIntegrationTests {

    private final ElectrumRegtestSetup electrumRegtestSetup =
            !WindowsElectrumRegtestSetup.isExternalBitcoindAndElectrumXEnvironment() ?
                    new MacLinuxElectrumRegtestSetup() :
                    new WindowsElectrumRegtestSetup();
    private ElectrumDaemon electrumDaemon;

    public ElectrumSetupIntegrationTests() throws IOException {
    }

    @BeforeAll
    void beforeAll() throws IOException, InterruptedException {
        electrumRegtestSetup.start();
        electrumDaemon = electrumRegtestSetup.getElectrumDaemon();
    }

    @AfterAll
    void afterAll() {
        electrumRegtestSetup.shutdown();
    }

    @Test
    void createAndLoadWalletTest() {
        ElectrumCreateResponse createResponse = electrumDaemon.create(MacLinuxElectrumRegtestSetup.WALLET_PASSPHRASE);

        Path electrumDataDir = electrumRegtestSetup.getElectrumDataDir();
        String absoluteDataDirPath = electrumDataDir.toAbsolutePath().toString();
        assertThat(createResponse.getPath()).startsWith(absoluteDataDirPath);

        String[] seedWords = createResponse.getSeed().split(" ");
        assertThat(seedWords.length).isEqualTo(12);

        electrumDaemon.loadWallet(MacLinuxElectrumRegtestSetup.WALLET_PASSPHRASE);
    }

    @Test
    void getInfoTest() {
        ElectrumGetInfoResponse info = electrumDaemon.getInfo();
        assertThat(info.isConnected()).isTrue();

        Path electrumDataDir = electrumRegtestSetup.getElectrumDataDir();
        String absoluteDataDirPath = electrumDataDir.toAbsolutePath().toString();

        assertThat(info.getDefaultWallet()).startsWith(absoluteDataDirPath);
        assertThat(info.getPath()).startsWith(absoluteDataDirPath);
    }
}
