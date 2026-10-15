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

import bisq.network.p2p.message.ReceiverPublicKeyProvidingPayload;
import bisq.network.p2p.message.SenderPublicKeyProvidingPayload;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfidentialMessageServiceTest {
    @Test
    void acceptsMatchingSenderPublicKey() {
        KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();

        assertDoesNotThrow(() -> ConfidentialMessageService.verifySenderPublicKeyBinding(
                new SenderPublicKeyPayload(senderKeyPair.getPublic()),
                senderKeyPair.getPublic().getEncoded()));
    }

    @Test
    void rejectsMismatchingSenderPublicKey() {
        KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyPair claimedSenderKeyPair = KeyGeneration.generateDefaultEcKeyPair();

        assertThrows(IllegalArgumentException.class, () -> ConfidentialMessageService.verifySenderPublicKeyBinding(
                new SenderPublicKeyPayload(claimedSenderKeyPair.getPublic()),
                senderKeyPair.getPublic().getEncoded()));
    }

    @Test
    void acceptsMatchingReceiverPublicKey() {
        KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();

        assertDoesNotThrow(() -> ConfidentialMessageService.verifyReceiverPublicKeyBinding(
                new ReceiverPublicKeyPayload(receiverKeyPair.getPublic()),
                receiverKeyPair.getPublic()));
    }

    @Test
    void rejectsMismatchingReceiverPublicKey() {
        KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyPair claimedReceiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();

        assertThrows(IllegalArgumentException.class, () -> ConfidentialMessageService.verifyReceiverPublicKeyBinding(
                new ReceiverPublicKeyPayload(claimedReceiverKeyPair.getPublic()),
                receiverKeyPair.getPublic()));
    }

    private record SenderPublicKeyPayload(PublicKey senderPublicKey) implements SenderPublicKeyProvidingPayload {
        @Override
        public PublicKey getSenderPublicKey() {
            return senderPublicKey;
        }
    }

    private record ReceiverPublicKeyPayload(PublicKey receiverPublicKey) implements ReceiverPublicKeyProvidingPayload {
        @Override
        public PublicKey getReceiverPublicKey() {
            return receiverPublicKey;
        }
    }
}
