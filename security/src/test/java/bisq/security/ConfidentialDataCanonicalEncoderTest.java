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

package bisq.security;

import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ConfidentialDataCanonicalEncoderTest {
    private static final byte[] PUBLIC_KEY_BYTES = KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded();
    private static final byte[] SIGNATURE = filledBytes(70, 0x22);

    // ConfidentialData has no field annotated with @ExcludeForHash, so every purpose gives the bytes of serialize()
    @Test
    public void everyPurposeMatchesProtobuf() {
        ConfidentialData confidentialData = new ConfidentialData(PUBLIC_KEY_BYTES,
                filledBytes(16, 0x33),
                filledBytes(40, 0x44),
                SIGNATURE);

        assertArrayEquals(confidentialData.serialize(), confidentialData.encodeCanonical());
        for (EncodingPurpose encodingPurpose : EncodingPurpose.values()) {
            assertArrayEquals(confidentialData.serializeForHash(), confidentialData.encodeCanonical(encodingPurpose),
                    encodingPurpose.name());
        }
    }

    @Test
    public void emptyValuesAreOmittedAsInProtobuf() {
        ConfidentialData confidentialData = new ConfidentialData(PUBLIC_KEY_BYTES,
                new byte[0],
                new byte[0],
                SIGNATURE);

        assertArrayEquals(confidentialData.serialize(), confidentialData.encodeCanonical());
    }

    private static byte[] filledBytes(int length, int value) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }
}
