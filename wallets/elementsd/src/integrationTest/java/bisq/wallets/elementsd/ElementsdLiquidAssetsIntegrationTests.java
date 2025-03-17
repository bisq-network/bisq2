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

package bisq.wallets.elementsd;

import bisq.wallets.core.model.AddressType;
import bisq.wallets.elementsd.regtest.ElementsdRegtestSetup;
import bisq.wallets.elementsd.rpc.responses.ElementsdIssueAssetResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

public class ElementsdLiquidAssetsIntegrationTests extends SharedElementsdInstanceTests {

    @Override
    @BeforeAll
    public void start() throws IOException, InterruptedException {
        super.start();
        peginBtc(20);
    }

    @Test
    void issueAsset() {
        elementsdMinerWallet.issueAsset(
                Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), 2, 1
        );
    }

    @Test
    public void sendOneLiquidAssetToAddress() throws InterruptedException {
        ElementsdIssueAssetResponse issueAssetResponse = elementsdMinerWallet.issueAsset(
                Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), 2, 1
        );
        String assetLabel = issueAssetResponse.getResult().getAsset();

        var receiverBackend = elementsdRegtestSetup.createNewWallet("receiver_wallet");

        String receiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        elementsdMinerWallet.sendAssetToAddress(
                Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), assetLabel, receiverAddress, 1
        );
        elementsdRegtestSetup.mineOneBlock();

        Assertions.assertThat(receiverBackend.getAssetBalance(assetLabel))
                .isEqualTo(1);
    }
}
