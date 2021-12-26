package network.misq.wallets.bitcoind;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoindIntegrationTests {
    @Test
    public void startAndStopTest() throws IOException {
        BitcoindProcess bitcoindProcess = BitcoindRegtestSetup.createAndStartBitcoind();
        boolean isSuccess = bitcoindProcess.stopAndWaitUntilStopped();
        assertTrue(isSuccess);
    }
}
