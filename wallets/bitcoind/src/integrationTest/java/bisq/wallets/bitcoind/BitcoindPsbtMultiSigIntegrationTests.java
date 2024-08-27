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

import bisq.wallets.bitcoind.regtest.BitcoindExtension;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.bitcoind.rpc.calls.requests.BitcoindImportDescriptorRequestEntry;
import bisq.wallets.bitcoind.rpc.responses.*;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.json_rpc.RpcConfig;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(BitcoindExtension.class)
public class BitcoindPsbtMultiSigIntegrationTests {

    private final BitcoindRegtestSetup regtestSetup;
    private final RpcConfig rpcConfig;
    private final BitcoindDaemon daemon;
    private final BitcoindWallet minerWallet;

    public BitcoindPsbtMultiSigIntegrationTests(BitcoindRegtestSetup regtestSetup) {
        this.regtestSetup = regtestSetup;
        this.rpcConfig = regtestSetup.getRpcConfig();
        this.daemon = regtestSetup.getDaemon();
        this.minerWallet = regtestSetup.getMinerWallet();
    }

    @Test
    public void psbtMultiSigTest() throws MalformedURLException, InterruptedException {
        var aliceWallet = regtestSetup.createAndInitializeNewWallet("alice_wallet");
        var bobWallet = regtestSetup.createAndInitializeNewWallet("bob_wallet");
        var charlieWallet = regtestSetup.createAndInitializeNewWallet("charlie_wallet");

        String aliceXPub = getWalletXPub(aliceWallet);
        String bobXPub = getWalletXPub(bobWallet);
        String charlieXPub = getWalletXPub(charlieWallet);

        BitcoindWallet aliceWatchOnlyWallet = createWatchOnlyDescriptorWallet("alice");
        BitcoindWallet bobWatchOnlyWallet = createWatchOnlyDescriptorWallet("bob");
        BitcoindWallet charlieWatchOnlyWallet = createWatchOnlyDescriptorWallet("charlie");

        String receiveDescriptor = "wsh(sortedmulti(2," +
                aliceXPub + "/0/*," +
                bobXPub + "/0/*," +
                charlieXPub + "/0/*))";
        String receiveDescriptorWithChecksum = appendChecksumToDescriptor(receiveDescriptor);

        var receiveDescriptorImportRequest = BitcoindImportDescriptorRequestEntry.builder()
                .desc(receiveDescriptorWithChecksum)
                .isActive(true)
                .isInternal(false)
                .build();

        aliceWatchOnlyWallet.importDescriptors(List.of(receiveDescriptorImportRequest));
        bobWatchOnlyWallet.importDescriptors(List.of(receiveDescriptorImportRequest));
        charlieWatchOnlyWallet.importDescriptors(List.of(receiveDescriptorImportRequest));

        String changeDescriptor = "wsh(sortedmulti(2," +
                aliceXPub + "/1/*," +
                bobXPub + "/1/*," +
                charlieXPub + "/1/*))";
        String changeDescriptorWithChecksum = appendChecksumToDescriptor(changeDescriptor);

        var changeDescriptorImportRequest = BitcoindImportDescriptorRequestEntry.builder()
                .desc(changeDescriptorWithChecksum)
                .isActive(true)
                .isInternal(true)
                .build();

        aliceWatchOnlyWallet.importDescriptors(List.of(changeDescriptorImportRequest));
        bobWatchOnlyWallet.importDescriptors(List.of(changeDescriptorImportRequest));
        charlieWatchOnlyWallet.importDescriptors(List.of(changeDescriptorImportRequest));

        String aliceAddress = aliceWatchOnlyWallet.getNewAddress(AddressType.BECH32, "");
        String bobAddress = bobWatchOnlyWallet.getNewAddress(AddressType.BECH32, "");
        String charlieAddress = charlieWatchOnlyWallet.getNewAddress(AddressType.BECH32, "");

        assertThat(aliceAddress).isEqualTo(bobAddress)
                .isEqualTo(charlieAddress);
        regtestSetup.fundAddress(aliceAddress, 5);

        assertThat(aliceWatchOnlyWallet.getBalance()).isEqualTo(5);
        assertThat(bobWatchOnlyWallet.getBalance()).isEqualTo(5);
        assertThat(charlieWatchOnlyWallet.getBalance()).isEqualTo(5);

        // Create PSBT (send to Alice without Alice)
        String aliceReceiveAddr = aliceWallet.getNewAddress(AddressType.BECH32, "");
        BitcoindWalletCreateFundedPsbtResponse.Result createFundedPsbtResponse = bobWatchOnlyWallet.walletCreateFundedPsbt(
                Collections.emptyList(),
                Map.of(aliceReceiveAddr, 4.),
                Map.of("feeRate", 0.00010)
        ).getResult();

        BitcoindWalletProcessPsbtResponse.Result bobPsbtResponse = bobWallet.walletProcessPsbt(
                Optional.of(AbstractRegtestSetup.WALLET_PASSPHRASE),
                createFundedPsbtResponse.getPsbt()
        ).getResult();
        BitcoindWalletProcessPsbtResponse.Result charliePsbtResponse = charlieWallet.walletProcessPsbt(
                Optional.of(AbstractRegtestSetup.WALLET_PASSPHRASE),
                createFundedPsbtResponse.getPsbt()
        ).getResult();

        // Combine PSBTs
        String combinedPsbt = daemon.combinePsbt(
                List.of(bobPsbtResponse.getPsbt(), charliePsbtResponse.getPsbt())
        );

        // Finalize PSBT
        BitcoindFinalizePsbtResponse.Result finalizePsbtResponse = daemon.finalizePsbt(combinedPsbt).getResult();
        assertThat(finalizePsbtResponse.isComplete())
                .isTrue();

        // Broadcast final transaction
        daemon.sendRawTransaction(finalizePsbtResponse.getHex());

        regtestSetup.mineOneBlock();
        assertThat(aliceWallet.getBalance())
                .isGreaterThan(3.9); // Not exactly 4.0 because of fees.
    }

    private String getWalletXPub(BitcoindWallet wallet) {
        List<BitcoindDescriptor> descriptors = wallet.listDescriptors().getResult().getDescriptors();
        String xPub = descriptors.stream()
                .map(BitcoindDescriptor::getDesc)
                .filter(descriptor -> descriptor.startsWith("pkh"))
                .findFirst()
                .orElseThrow();
        return xPub.split("]")[1].split("/")[0];
    }

    private BitcoindWallet createWatchOnlyDescriptorWallet(String walletPrefix) {
        String walletName = walletPrefix + "_watch_only_descriptor_wallet";
        daemon.createOrLoadWatchOnlyWallet(walletName);
        return new BitcoindWallet(daemon, rpcConfig, walletName);
    }

    private String appendChecksumToDescriptor(String descriptor) {
        BitcoindGetDescriptorInfoResponse.Result receiveDescriptorInfo = minerWallet.getDescriptorInfo(descriptor).getResult();
        return receiveDescriptorInfo.getDescriptor();
    }
}
