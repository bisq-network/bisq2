package bisq.wallets.bitcoind;

import bisq.wallets.AddressType;
import bisq.wallets.bitcoind.psbt.PsbtOptions;
import bisq.wallets.bitcoind.psbt.PsbtOutput;
import bisq.wallets.bitcoind.responses.*;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoindPsbtMultiSigIntegrationTests extends SharedBitcoindInstanceTests {

    @Test
    public void psbtMultiSigTest() throws MalformedURLException {
        BitcoindRegtestSetup.mineInitialRegtestBlocks(minerChainBackend, minerWalletBackend);

        var aliceBackend = BitcoindRegtestSetup
                .createTestWalletBackend(minerChainBackend, tmpDirPath, "alice_wallet");
        var bobBackend = BitcoindRegtestSetup
                .createTestWalletBackend(minerChainBackend, tmpDirPath, "bob_wallet");
        var charlieBackend = BitcoindRegtestSetup
                .createTestWalletBackend(minerChainBackend, tmpDirPath, "charlie_wallet");

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
