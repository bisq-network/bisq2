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

package bisq.network.p2p.services.data.storage;

import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.AT_CUTOFF;
import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.BEFORE_CUTOFF;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verification rule 3 of docs/specifications/network/storage-request-signatures.md, which the node checks when it
 * parses a request. Before the cutoff date, a request must contain signature V1, also if it contains signature V2:
 * nodes of earlier versions cannot parse a request without signature V1.
 */
class SignatureValidationTest {
    private static final byte[] NO_SIGNATURE = new byte[0];
    private static final byte[] INVALID_ENCODING = new byte[10];

    private byte[] signatureV1;
    private byte[] signatureV2;

    @BeforeEach
    void setUp() throws GeneralSecurityException {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        signatureV1 = SignatureUtil.sign("preimage V1".getBytes(StandardCharsets.UTF_8), keyPair.getPrivate());
        signatureV2 = SignatureUtil.sign("preimage V2".getBytes(StandardCharsets.UTF_8), keyPair.getPrivate());
    }

    @Test
    void beforeTheCutoffDateSignatureV1IsRequiredAlsoWithSignatureV2() {
        assertThrows(IllegalArgumentException.class,
                () -> SignatureValidation.validateSignatures(NO_SIGNATURE, signatureV2, BEFORE_CUTOFF));
        assertDoesNotThrow(() -> SignatureValidation.validateSignatures(signatureV1, signatureV2, BEFORE_CUTOFF));
        assertDoesNotThrow(() -> SignatureValidation.validateSignatures(signatureV1, NO_SIGNATURE, BEFORE_CUTOFF));
    }

    @Test
    void fromTheCutoffDateOnSignatureV2IsRequiredAndSignatureV1IsOptional() {
        assertThrows(IllegalArgumentException.class,
                () -> SignatureValidation.validateSignatures(signatureV1, NO_SIGNATURE, AT_CUTOFF));
        assertDoesNotThrow(() -> SignatureValidation.validateSignatures(NO_SIGNATURE, signatureV2, AT_CUTOFF));
        assertDoesNotThrow(() -> SignatureValidation.validateSignatures(signatureV1, signatureV2, AT_CUTOFF));
    }

    @Test
    void anOptionalSignatureWhichIsPresentMustHaveAValidEncoding() {
        assertThrows(IllegalArgumentException.class,
                () -> SignatureValidation.validateSignatures(signatureV1, INVALID_ENCODING, BEFORE_CUTOFF));
        assertThrows(IllegalArgumentException.class,
                () -> SignatureValidation.validateSignatures(INVALID_ENCODING, signatureV2, AT_CUTOFF));
    }
}
