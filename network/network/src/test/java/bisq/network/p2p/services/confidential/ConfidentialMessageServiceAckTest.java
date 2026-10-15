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
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.NodesById;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatusService;
import bisq.security.HybridEncryption;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The MessageDeliveryStatusService sends the AckMessage from our identity whose key decrypted the message. Only the
 * ConfidentialMessageService knows that key, so it passes the key ID with each decrypted message.
 */
@Isolated("Uses the static NetworkExecutors")
class ConfidentialMessageServiceAckTest {
    private static final String KEY_ID_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String KEY_ID_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final int TIMEOUT_MS = 5000;

    private final KeyPair keyPairA = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair keyPairB = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair peerKeyPair = KeyGeneration.generateDefaultEcKeyPair();

    private MessageDeliveryStatusService messageDeliveryStatusService;
    private ConfidentialMessageService confidentialMessageService;

    @BeforeEach
    void setUp() {
        NetworkExecutors.shutdown();
        NetworkExecutors.initialize(4);

        KeyBundleService keyBundleService = mock(KeyBundleService.class);
        when(keyBundleService.findKeyPair(KEY_ID_A)).thenReturn(Optional.of(keyPairA));
        when(keyBundleService.findKeyPair(KEY_ID_B)).thenReturn(Optional.of(keyPairB));
        messageDeliveryStatusService = mock(MessageDeliveryStatusService.class);
        confidentialMessageService = new ConfidentialMessageService(mock(NodesById.class),
                keyBundleService,
                Optional.empty(),
                Optional.of(messageDeliveryStatusService));
    }

    @AfterEach
    void tearDown() {
        NetworkExecutors.shutdown();
    }

    @Test
    void ackServiceGetsTheKeyIdOfTheDecryptingKeyForEachOfOurKeys() throws GeneralSecurityException {
        AckMessage message = new AckMessage("message1");

        receive(message, keyPairA, KEY_ID_A);
        verify(messageDeliveryStatusService, timeout(TIMEOUT_MS)).onDecryptedMessage(message, KEY_ID_A);

        // The same message for our key B is processed again, as a separate node would do
        receive(message, keyPairB, KEY_ID_B);
        verify(messageDeliveryStatusService, timeout(TIMEOUT_MS)).onDecryptedMessage(message, KEY_ID_B);

        // Another copy for key A is a duplicate. The service processes the messages in order, so after the next
        // message arrived, the duplicate was processed and not passed on.
        receive(message, keyPairA, KEY_ID_A);
        AckMessage nextMessage = new AckMessage("message2");
        receive(nextMessage, keyPairA, KEY_ID_A);
        verify(messageDeliveryStatusService, timeout(TIMEOUT_MS)).onDecryptedMessage(nextMessage, KEY_ID_A);
        verify(messageDeliveryStatusService, times(1)).onDecryptedMessage(message, KEY_ID_A);
    }

    private void receive(EnvelopePayloadMessage message, KeyPair receiverKeyPair, String receiverKeyId)
            throws GeneralSecurityException {
        ConfidentialMessage confidentialMessage = new ConfidentialMessage(
                HybridEncryption.encryptAndSign(message.serialize(), receiverKeyPair.getPublic(), peerKeyPair),
                receiverKeyId);
        confidentialMessageService.onMessage(confidentialMessage, mock(Connection.class), mock(NetworkId.class));
    }
}
