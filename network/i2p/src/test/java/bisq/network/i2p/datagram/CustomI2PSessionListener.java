package bisq.network.i2p.datagram;

import lombok.extern.slf4j.Slf4j;
import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.client.I2PSessionListener;
import net.i2p.data.DataHelper;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class CustomI2PSessionListener implements I2PSessionListener {

    private final CountDownLatch latch;

    public CustomI2PSessionListener(CountDownLatch latch) {
        this.latch = latch;
    }

    public void messageAvailable(I2PSession session, int msgId, long size) {
        log.info("Message available with ID {}", msgId);

        try {
            byte[] msgBytes = session.receiveMessage(msgId);
            String msg = DataHelper.getUTF8(msgBytes);
            log.info("Message is: {}", msg);

            latch.countDown();
        } catch (I2PSessionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reportAbuse(I2PSession session, int severity) {
    }

    @Override
    public void disconnected(I2PSession session) {
        log.info("Disconnected");
    }

    @Override
    public void errorOccurred(I2PSession session, String message, Throwable error) {
        log.error("Error occurred: {}", message);
    }
}