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

package bisq.network.p2p.services.confidential;

import bisq.network.NetworkExecutors;
import bisq.network.NetworkService;
import bisq.network.StartupReplay;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.NodesById;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.security.HybridEncryption;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.KeyGeneration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A service which registers receives the confidential messages which the node processed before, exactly once, and a
 * message which the service cannot process does not stop the others (see
 * docs/specifications/network/startup-replay.md).
 */
@Isolated("Uses the static NetworkExecutors and captures the logs of ConfidentialMessageService and StartupReplay")
class ConfidentialMessageServiceDeliveryTest {
    private static final String KEY_ID = "a".repeat(40);
    private static final long TIMEOUT_SECONDS = 10;

    private final KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final Logger logger = (Logger) LoggerFactory.getLogger(ConfidentialMessageService.class);
    private final Logger replayLogger = (Logger) LoggerFactory.getLogger(StartupReplay.class);
    private ConfidentialMessageService confidentialMessageService;

    @BeforeEach
    void setUp() {
        NetworkExecutors.shutdown();
        NetworkExecutors.initialize(4);

        KeyBundleService keyBundleService = mock(KeyBundleService.class);
        when(keyBundleService.findKeyPair(KEY_ID)).thenReturn(Optional.of(keyPair));
        confidentialMessageService = new ConfidentialMessageService(mock(NodesById.class),
                keyBundleService,
                Optional.empty(),
                Optional.empty());
    }

    @AfterEach
    void tearDown() {
        NetworkExecutors.shutdown();
    }

    @Test
    void listenerReceivesTheMessagesProcessedBeforeItWasAddedInTheirOrder() throws Exception {
        AckMessage first = new AckMessage("message1");
        AckMessage second = new AckMessage("message2");
        receiveAndAwaitProcessing(first, second);
        RecordingListener listener = new RecordingListener();

        confidentialMessageService.subscribe(listener);

        // The messages are passed before the method returns
        assertEquals(List.of(first, second), listener.messages);
        assertEquals(List.of(first, second), listener.confidentialMessages);
        assertEquals(List.of(senderKeyPair.getPublic(), senderKeyPair.getPublic()), listener.senderPublicKeys);
    }

    @Test
    void listenerReceivesEachMessageOnceAlsoWhenItIsAddedAgain() throws Exception {
        AckMessage processedBefore = new AckMessage("message1");
        AckMessage processedAfter = new AckMessage("message2");
        receiveAndAwaitProcessing(processedBefore);
        RecordingListener listener = new RecordingListener();
        confidentialMessageService.subscribe(listener);

        receiveAndAwaitProcessing(processedAfter);
        confidentialMessageService.subscribe(listener);
        awaitNotifications();

        assertEquals(List.of(processedBefore, processedAfter), listener.messages);
        assertEquals(List.of(processedBefore, processedAfter), listener.confidentialMessages);
    }

    @Test
    void aMessageWhichTheListenerCannotProcessDoesNotStopTheOthers() throws Exception {
        AckMessage first = new AckMessage("message1");
        AckMessage failing = new AckMessage("failingMessage");
        AckMessage last = new AckMessage("message3");
        receiveAndAwaitProcessing(first, failing, last);
        RecordingListener listener = new RecordingListener(Set.of(failing));
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        replayLogger.addAppender(appender);
        try {
            assertDoesNotThrow(() -> confidentialMessageService.subscribe(listener));
        } finally {
            replayLogger.detachAppender(appender);
            appender.stop();
        }

        assertEquals(List.of(first, failing, last), listener.messages);
        // The call with the sender key is independent of the failed call, as for a new message
        assertEquals(List.of(first, failing, last), listener.confidentialMessages);

        List<ILoggingEvent> errors = appender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .toList();
        assertEquals(1, errors.size());
        // Logged as the other replayed data: the type of the message and the exception
        ILoggingEvent error = errors.get(0);
        assertTrue(error.getFormattedMessage().contains(AckMessage.class.getSimpleName()));
        // The log names the type of the message, not its content
        assertFalse(error.getFormattedMessage().contains(failing.getId()));
        assertInstanceOf(IllegalStateException.class, ((ThrowableProxy) error.getThrowableProxy()).getThrowable());
    }

    // A message which the node processes while a listener is added must reach the listener once: either in the copy of
    // the processed messages or as a new message. The test stops the processing thread inside the lock, after it added
    // the message to the processed messages and before it notified the listeners.
    @Test
    void aMessageProcessedWhileTheListenerIsAddedIsDeliveredExactlyOnce() throws Exception {
        AckMessage message = new AckMessage("message1");
        AckMessage nextMessage = new AckMessage("message2");
        RecordingListener listener = new RecordingListener();
        PausingAppender pausingAppender = new PausingAppender();
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        pausingAppender.start();
        logger.addAppender(pausingAppender);
        try {
            receive(message);
            assertTrue(pausingAppender.paused.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            Thread addingThread = new Thread(() -> confidentialMessageService.subscribe(listener));
            addingThread.start();
            // The adding thread waits for the lock which the processing thread holds
            awaitBlockedOrTerminated(addingThread);
            pausingAppender.release.countDown();
            addingThread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            assertFalse(addingThread.isAlive());

            // One thread processes the messages in their order. When the listener has the next message, the
            // notification of the first message was submitted.
            receive(nextMessage);
            await(() -> listener.messages.contains(nextMessage));
            awaitNotifications();
        } finally {
            pausingAppender.release.countDown();
            logger.detachAppender(pausingAppender);
            pausingAppender.stop();
            logger.setLevel(previousLevel);
        }

        assertEquals(1, Collections.frequency(listener.messages, message));
        assertEquals(1, Collections.frequency(listener.confidentialMessages, message));
    }

    // The confidential message services must add the listener before the service nodes add it, otherwise it would
    // not receive the processed messages. The service nodes keep it for removeConfidentialMessageListener.
    @Test
    void networkServiceDeliversTheProcessedMessagesAndAddsTheListenerToTheServiceNodes() throws Exception {
        AckMessage message = new AckMessage("message1");
        receiveAndAwaitProcessing(message);
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getConfidentialMessageServices()).thenReturn(Set.of(confidentialMessageService));
        doCallRealMethod().when(networkService).subscribeToConfidentialMessages(any());
        // The service nodes add the listener to the confidential message services as well
        doAnswer(invocation -> {
            confidentialMessageService.addListener(invocation.getArgument(0));
            return null;
        }).when(networkService).addConfidentialMessageListener(any());
        RecordingListener listener = new RecordingListener();

        networkService.subscribeToConfidentialMessages(listener);

        assertEquals(List.of(message), listener.messages);
        verify(networkService).addConfidentialMessageListener(listener);
    }

    private void receiveAndAwaitProcessing(EnvelopePayloadMessage... messages) throws Exception {
        CountDownLatch processed = new CountDownLatch(messages.length);
        ConfidentialMessageService.Listener counter = message -> processed.countDown();
        confidentialMessageService.addListener(counter);
        for (EnvelopePayloadMessage message : messages) {
            receive(message);
        }
        assertTrue(processed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        confidentialMessageService.removeListener(counter);
    }

    private void receive(EnvelopePayloadMessage message) throws GeneralSecurityException {
        ConfidentialMessage confidentialMessage = new ConfidentialMessage(
                HybridEncryption.encryptAndSign(message.serialize(), keyPair.getPublic(), senderKeyPair),
                KEY_ID);
        confidentialMessageService.onMessage(confidentialMessage, mock(Connection.class), mock(NetworkId.class));
    }

    // The executor terminates after all submitted notifications ran
    private static void awaitNotifications() throws InterruptedException {
        ThreadPoolExecutor executor = NetworkExecutors.getNotifyExecutor();
        executor.shutdown();
        assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private static void awaitBlockedOrTerminated(Thread thread) throws InterruptedException {
        await(() -> thread.getState() == Thread.State.BLOCKED || thread.getState() == Thread.State.TERMINATED);
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (!condition.getAsBoolean()) {
            assertTrue(System.currentTimeMillis() < deadline, "Condition not met in time");
            Thread.sleep(1);
        }
    }

    private static final class RecordingListener implements ConfidentialMessageService.Listener {
        private final Set<EnvelopePayloadMessage> failingMessages;
        private final List<EnvelopePayloadMessage> messages = new CopyOnWriteArrayList<>();
        private final List<EnvelopePayloadMessage> confidentialMessages = new CopyOnWriteArrayList<>();
        private final List<PublicKey> senderPublicKeys = new CopyOnWriteArrayList<>();

        private RecordingListener() {
            this(Set.of());
        }

        private RecordingListener(Set<EnvelopePayloadMessage> failingMessages) {
            this.failingMessages = failingMessages;
        }

        @Override
        public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
            messages.add(envelopePayloadMessage);
            if (failingMessages.contains(envelopePayloadMessage)) {
                throw new IllegalStateException("Listener failed");
            }
        }

        @Override
        public void onConfidentialMessage(EnvelopePayloadMessage envelopePayloadMessage, PublicKey senderPublicKey) {
            confidentialMessages.add(envelopePayloadMessage);
            senderPublicKeys.add(senderPublicKey);
        }
    }

    // Stops the first thread which writes the log entry about a decrypted message, until the test releases it
    private static final class PausingAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
        private final CountDownLatch paused = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicBoolean hasPaused = new AtomicBoolean();

        @Override
        protected void append(ILoggingEvent event) {
            if (event.getFormattedMessage().startsWith("Decrypted confidentialMessage") &&
                    hasPaused.compareAndSet(false, true)) {
                paused.countDown();
                try {
                    release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
