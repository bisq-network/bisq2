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

import bisq.wallets.AddressType;
import bisq.wallets.bitcoind.psbt.PsbtOptions;
import bisq.wallets.bitcoind.psbt.PsbtOutput;
import bisq.wallets.bitcoind.responses.*;
import bisq.wallets.bitcoind.rpc.RpcConfig;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoindPsbtMultiSigIntegrationTests extends SharedBitcoindInstanceTests {

    @Test
    public void psbtMultiSigTest() throws MalformedURLException {
        BitcoindRegtestSetup.mineInitialRegtestBlocks(minerChainBackend, minerWalletBackend);
        RpcConfig rpcConfig = bitcoindProcess.getRpcConfig();

        var aliceBackend = BitcoindRegtestSetup
                .createTestWalletBackend(rpcConfig, minerChainBackend, tmpDirPath, "alice_wallet");
        var bobBackend = BitcoindRegtestSetup
                .createTestWalletBackend(rpcConfig, minerChainBackend, tmpDirPath, "bob_wallet");
        var charlieBackend = BitcoindRegtestSetup
                .createTestWalletBackend(rpcConfig, minerChainBackend, tmpDirPath, "charlie_wallet");

        String aliceAddress = aliceBackend.getNewAddress(AddressType.BECH32, "");
        String bobAddress = bobBackend.getNewAddress(AddressType.BECH32, "");
        String charlieAddress = charlieBackend.getNewAddress(AddressType.BECH32, "");

        GetAddressInfoResponse aliceAddrInfo = aliceBackend.getAddressInfo(aliceAddress);
        GetAddressInfoResponse bobAddrInfo = bobBackend.getAddressInfo(bobAddress);
        GetAddressInfoResponse charlieAddrInfo = charlieBackend.getAddressInfo(charlieAddress);

        // Generate MultiSig Address
        var keys = new ArrayList<String>();
        keys.add(aliceAddrInfo.getPubkey());
        keys.add(bobAddrInfo.getPubkey());
        keys.add(charlieAddrInfo.getPubkey());

        AddMultisigAddressResponse aliceMultiSigAddrResponse = aliceBackend.addMultisigAddress(2, keys);
        AddMultisigAddressResponse bobMultiSigAddrResponse = bobBackend.addMultisigAddress(2, keys);
        AddMultisigAddressResponse charlieMultiSigAddrResponse = charlieBackend.addMultisigAddress(2, keys);

        aliceBackend.importAddress(aliceMultiSigAddrResponse.getAddress(), "");
        bobBackend.importAddress(bobMultiSigAddrResponse.getAddress(), "");
        charlieBackend.importAddress(charlieMultiSigAddrResponse.getAddress(), "");

        minerWalletBackend.sendToAddress(aliceMultiSigAddrResponse.getAddress(), 5);
        BitcoindRegtestSetup.mineOneBlock(minerChainBackend, minerWalletBackend);

        // Create PSBT (send to Alice without Alice)
        String aliceReceiveAddr = aliceBackend.getNewAddress(AddressType.BECH32, "");
        PsbtOutput psbtOutput = new PsbtOutput();
        psbtOutput.addOutput(aliceReceiveAddr, 4d);

        var psbtOptions = new PsbtOptions(
                true,
                new int[]{0}
        );

        WalletCreateFundedPsbtResponse createFundedPsbtResponse = bobBackend.walletCreateFundedPsbt(
                Collections.emptyList(),
                psbtOutput,
                0,
                psbtOptions
        );

        // Bob and Charlie sign the PSBT
        WalletProcessPsbtResponse bobPsbtResponse = bobBackend.walletProcessPsbt(createFundedPsbtResponse.getPsbt());
        WalletProcessPsbtResponse charliePsbtResponse = charlieBackend.walletProcessPsbt(bobPsbtResponse.getPsbt());

        // Finalize PSBT
        FinalizePsbtResponse finalizePsbtResponse = minerChainBackend.finalizePsbt(charliePsbtResponse.getPsbt());
        assertTrue(finalizePsbtResponse.isComplete());

        // Broadcast final transaction
        minerChainBackend.sendRawTransaction(finalizePsbtResponse.getHex());

        BitcoindRegtestSetup.mineOneBlock(minerChainBackend, minerWalletBackend);
        assertTrue(aliceBackend.getBalance() > 3.9); // Not exactly 4.0 because of fees.
    }
}
