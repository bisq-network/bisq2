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

package bisq.contract;

import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractServiceTest {
    @Test
    void detectsMatchingSignaturePublicKey() {
        ContractService contractService = new ContractService(null);
        KeyPair signerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        ContractSignatureData signatureData = new ContractSignatureData(
                new byte[20],
                new byte[68],
                signerKeyPair.getPublic());

        assertTrue(contractService.isSignaturePublicKey(signatureData, signerKeyPair.getPublic()));
    }

    @Test
    void detectsMismatchingSignaturePublicKey() {
        ContractService contractService = new ContractService(null);
        KeyPair signerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyPair claimedPartyKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        ContractSignatureData signatureData = new ContractSignatureData(
                new byte[20],
                new byte[68],
                signerKeyPair.getPublic());

        assertFalse(contractService.isSignaturePublicKey(signatureData, claimedPartyKeyPair.getPublic()));
    }
}
