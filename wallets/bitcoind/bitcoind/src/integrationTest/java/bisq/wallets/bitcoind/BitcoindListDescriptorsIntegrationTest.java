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

import bisq.wallets.regtest.BitcoindExtension;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.bitcoind.rpc.responses.BitcoindDescriptor;
import bisq.wallets.bitcoind.rpc.responses.BitcoindListDescriptorResponse;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(BitcoindExtension.class)
public class BitcoindListDescriptorsIntegrationTest {
    private final BitcoindWallet minerWallet;

    public BitcoindListDescriptorsIntegrationTest(BitcoindRegtestSetup regtestSetup) {
        this.minerWallet = regtestSetup.getMinerWallet();
    }

    @Test
    void listDescriptorsTest() {
        BitcoindListDescriptorResponse.Result response = minerWallet.listDescriptors().getResult();
        List<BitcoindDescriptor> descriptorList = response.getDescriptors();

        assertThat(descriptorList).isNotEmpty()
                .anySatisfy(descriptor -> assertThat(descriptor.getDesc()).startsWith("pkh(["));
    }
}
