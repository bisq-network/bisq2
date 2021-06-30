package network.misq.tor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MINUTES;
import static network.misq.tor.Constants.VERSION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AsyncBootstrapIntegrationTest extends AbstractTorTest {

    @Test
    @Order(1)
    public void testAsyncBootstrap() {
        try {
            String torDirPathSpec = torTestDirPathSpec.get();
            cleanTorInstallDir(torDirPathSpec);
            tor = Tor.getTor(torDirPathSpec);

            CountDownLatch latch = new CountDownLatch(1);
            tor.startAsync()
                    .thenCompose(result -> startServerAsync()
                            .thenAccept(onionAddress -> {
                                if (onionAddress == null) {
                                    return;
                                }
                                sendViaSocketFactory(tor, onionAddress);
                                sendViaProxy(tor, onionAddress);
                                sendViaSocket(tor, onionAddress);
                                sendViaSocksSocket(tor, onionAddress);
                                latch.countDown();
                            }));
            //noinspection ResultOfMethodCallIgnored
            latch.await(2, MINUTES);

            shutdownTor();
        } catch (InterruptedException ignored) {
            // empty
        }
    }

    @Test
    @Order(2)
    public void testShutdownDuringStartup() {
        String torDirPathSpec = torTestDirPathSpec.get();
        cleanTorInstallDir(torDirPathSpec);
        tor = Tor.getTor(torDirPathSpec);

        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
                // empty
            }
            tor.shutdown();
        }).start();

        Thread mainThread = Thread.currentThread();
        tor.startAsync()
                .exceptionally(throwable -> {
                    File versionFile = new File(torDirPathSpec + File.separator + VERSION);
                    assertFalse(versionFile.exists());
                    mainThread.interrupt();
                    return null;
                })
                .thenAccept(result -> {
                    if (result == null) {
                        return;
                    }
                    fail();
                });
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
            // empty
        }
    }

    private void shutdownTor() {
        try {
            isShutdown = true;
            tor.getTorServerSocket().close();
            tor.shutdown();
        } catch (IOException ex) {
            fail("Error during Tor shutdown.", ex);
        }
    }
}
