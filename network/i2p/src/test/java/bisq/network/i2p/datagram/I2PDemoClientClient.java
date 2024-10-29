package bisq.network.i2p.datagram;

import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import net.i2p.I2PException;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.data.Destination;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

// Requires local i2p installation
// Start I2P.
// Does NOT require SAM bridge
// Takes about 1 minute until its ready (green icon next to "shared clients")
@Slf4j
public class I2PDemoClientClient {

    /**
     * From <a href="https://geti2p.net/en/get-involved/develop/applications">...</a> :
     * <p>
     * "An application that needs a simple request and response can get rid of any state and drop the latency incurred by
     * the startup and tear down handshakes by using (the best effort) datagrams without having to worry about MTU detection
     * or fragmentation of messages."
     */
    public static void main(String[] args) throws I2PException, IOException, InterruptedException {

        ByteArrayOutputStream outAlice = new ByteArrayOutputStream();
        I2PClient clientAlice = I2PClientFactory.createClient();
        Destination destinationAlice = clientAlice.createDestination(outAlice);
        I2PSession sessionAlice = clientAlice.createSession(new ByteArrayInputStream(outAlice.toByteArray()), null);
        log.info("Destination Alice in base64: {}", sessionAlice.getMyDestination().toBase64());

        ByteArrayOutputStream outBob = new ByteArrayOutputStream();
        I2PClient clientBob = I2PClientFactory.createClient();
        Destination destinationBob = clientBob.createDestination(outBob);
        I2PSession sessionBob = clientBob.createSession(new ByteArrayInputStream(outBob.toByteArray()), null);
        log.info("Destination Bob in base64: {}", sessionBob.getMyDestination().toBase64());

        sessionAlice.connect();
        log.info("Opened Alice session");
        sessionBob.connect();
        log.info("Opened Bob session");

        CountDownLatch latch = new CountDownLatch(2);

        // TODO payload sent unencrypted?
        // TODO method that specifies the protocol (datagram/streaming) and ports
        // For datagrams, see https://geti2p.net/spec/datagrams
        // For streaming, see https://geti2p.net/en/docs/api/streaming
        sessionAlice.sendMessage(destinationBob, "Hello from Alice to Bob".getBytes(Charsets.UTF_8));
        sessionBob.sendMessage(destinationAlice, "Hello from Bob to Alice".getBytes(Charsets.UTF_8));

        sessionBob.setSessionListener(new CustomI2PSessionListener(latch));
        sessionAlice.setSessionListener(new CustomI2PSessionListener(latch));

        // Wait until msgs were received
        latch.await();

        sessionAlice.destroySession();
        log.info("Closed Alice session");
        sessionBob.destroySession();
        log.info("Closed Bob session");
    }
}