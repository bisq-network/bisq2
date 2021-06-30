package network.misq.tor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockingBootstrapIntegrationTest extends AbstractTorTest {

    @Test
    @Order(1)
    public void testBlockingBootstrap() {
        try {
            String torDirPathSpec = torTestDirPathSpec.get();
            cleanTorInstallDir(torDirPathSpec);

            tor = Tor.getTor(torDirPathSpec);
            tor.start();
            torServerSocket = startServer();
            onionAddress = torServerSocket.getOnionAddress()
                    .orElseThrow(() -> new IllegalStateException("Could not get onion address from tor server socket."));
        } catch (IOException ex) {
            fail(ex);
        } catch (InterruptedException ignored) {
            // empty
        }
    }

    @Test
    @Order(2)
    public void testSendMessageViaSocketFactory() {
        sendViaSocketFactory(tor, onionAddress);
    }

    @Test
    @Order(3)
    public void testSendMessageViaProxy() {
        sendViaProxy(tor, onionAddress);
    }

    @Test
    @Order(4)
    public void testSendMessageViaSocket() {
        sendViaSocket(tor, onionAddress);
    }

    @Test
    @Order(5)
    public void testSendMessageViaSocksSocket() {
        sendViaSocksSocket(tor, onionAddress);
    }

    @AfterAll
    public static void shutdownTor() {
        try {
            Thread.sleep(5000);
            isShutdown = true;
            torServerSocket.close();
            tor.shutdown();
        } catch (IOException ex) {
            fail("Error during Tor shutdown.", ex);
        } catch (InterruptedException ignored) {
            // empty
        }
    }
}
