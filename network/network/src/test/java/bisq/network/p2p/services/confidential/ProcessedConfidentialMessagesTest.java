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

import bisq.network.p2p.message.EnvelopePayloadMessage;
import com.google.protobuf.Message;
import bisq.network.p2p.node.NodesById;
import bisq.network.p2p.services.confidential.ConfidentialMessageService.EnvelopePayloadMessageAndPublicKey;
import bisq.security.keys.KeyBundleService;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static bisq.network.p2p.services.confidential.ConfidentialMessageService.MAX_PROCESSED_MESSAGES;
import static bisq.network.p2p.services.confidential.ConfidentialMessageService.MAX_PROCESSED_MESSAGES_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ProcessedConfidentialMessagesTest {
    private static final int SIZE = 100;

    private final ConfidentialMessageService service = new ConfidentialMessageService(mock(NodesById.class),
            mock(KeyBundleService.class),
            Optional.empty(),
            Optional.empty());
    private final PublicKey senderPublicKey = mock(PublicKey.class);

    @Test
    void messageProcessedAgainIsADuplicate() {
        EnvelopePayloadMessageAndPublicKey message = message();

        assertTrue(service.addProcessedMessage(message, SIZE));
        assertFalse(service.addProcessedMessage(message, SIZE));
        assertEquals(List.of(message.envelopePayloadMessage()), replayedMessages());
    }

    @Test
    void oldestMessageIsDroppedAtTheBound() {
        List<EnvelopePayloadMessage> messages = new ArrayList<>();
        for (int i = 0; i <= MAX_PROCESSED_MESSAGES; i++) {
            EnvelopePayloadMessageAndPublicKey message = message();
            messages.add(message.envelopePayloadMessage());
            service.addProcessedMessage(message, SIZE);
        }

        assertEquals(messages.subList(1, MAX_PROCESSED_MESSAGES + 1), replayedMessages());
    }

    @Test
    void duplicateDoesNotKeepTheOldestMessage() {
        EnvelopePayloadMessageAndPublicKey oldest = message();
        service.addProcessedMessage(oldest, SIZE);
        for (int i = 1; i < MAX_PROCESSED_MESSAGES; i++) {
            service.addProcessedMessage(message(), SIZE);
        }

        service.addProcessedMessage(oldest, SIZE);
        service.addProcessedMessage(message(), SIZE);

        assertFalse(replayedMessages().contains(oldest.envelopePayloadMessage()));
    }

    @Test
    void oldestMessagesAreDroppedAtTheSizeBound() {
        int size = (int) (MAX_PROCESSED_MESSAGES_SIZE / 4);
        List<EnvelopePayloadMessage> messages = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            EnvelopePayloadMessageAndPublicKey message = message();
            messages.add(message.envelopePayloadMessage());
            service.addProcessedMessage(message, size);
        }

        assertEquals(messages.subList(2, 6), replayedMessages());
    }

    @Test
    void messageLargerThanTheSizeBoundIsKept() {
        EnvelopePayloadMessageAndPublicKey older = message();
        EnvelopePayloadMessageAndPublicKey large = message();
        service.addProcessedMessage(older, SIZE);

        assertTrue(service.addProcessedMessage(large, (int) MAX_PROCESSED_MESSAGES_SIZE + 1));
        assertEquals(List.of(large.envelopePayloadMessage()), replayedMessages());
    }

    // The startup replay must not depend on the hash codes of received messages, so neither may the processed messages
    @Test
    void messageWhoseHashCodeThrowsIsProcessed() {
        EnvelopePayloadMessageAndPublicKey message = new EnvelopePayloadMessageAndPublicKey(new MessageWhoseHashCodeThrows(),
                senderPublicKey,
                "receiverKeyId");

        assertTrue(service.addProcessedMessage(message, SIZE));
        assertFalse(service.addProcessedMessage(message, SIZE));
        assertEquals(List.of(message.envelopePayloadMessage()), replayedMessages());
    }

    @Test
    void messageWithSenderKeyWhoseHashCodeThrowsIsProcessed() {
        EnvelopePayloadMessageAndPublicKey message = new EnvelopePayloadMessageAndPublicKey(mock(EnvelopePayloadMessage.class),
                new PublicKeyWhoseHashCodeThrows(),
                "receiverKeyId");

        assertTrue(service.addProcessedMessage(message, SIZE));
        assertFalse(service.addProcessedMessage(message, SIZE));
        assertEquals(List.of(message.envelopePayloadMessage()), replayedMessages());
    }

    private EnvelopePayloadMessageAndPublicKey message() {
        return new EnvelopePayloadMessageAndPublicKey(mock(EnvelopePayloadMessage.class), senderPublicKey, "receiverKeyId");
    }

    // The messages which a listener receives when it subscribes, in the order of processing
    private List<EnvelopePayloadMessage> replayedMessages() {
        List<EnvelopePayloadMessage> replayed = new ArrayList<>();
        service.subscribe(new ConfidentialMessageService.Listener() {
            @Override
            public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
                replayed.add(envelopePayloadMessage);
            }
        });
        return replayed;
    }

    private static class MessageWhoseHashCodeThrows implements EnvelopePayloadMessage {
        @Override
        public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Message.Builder getValueBuilder(boolean serializeForHash) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void verify() {
        }

        @Override
        public int hashCode() {
            throw new IllegalStateException("hashCode of a received message");
        }
    }

    private static class PublicKeyWhoseHashCodeThrows implements PublicKey {
        @Override
        public String getAlgorithm() {
            return "test";
        }

        @Override
        public String getFormat() {
            return "test";
        }

        @Override
        public byte[] getEncoded() {
            return new byte[0];
        }

        @Override
        public int hashCode() {
            throw new IllegalStateException("hashCode of a received message");
        }
    }
}
