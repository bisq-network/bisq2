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

package bisq.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkDataValidationTest {
    // The rejected values come from other nodes, for example a chat text or payment account data, and the callers log
    // the exception messages. A message names the limit and the size of the value, not the value.
    private static final String SECRET = "IBAN DE89370400440532013000";

    @Test
    void exceptionMessagesDoNotContainTheRejectedValue() {
        String longSecret = SECRET.repeat(10);
        byte[] secretBytes = SECRET.getBytes();
        List<Executable> validations = List.of(
                () -> NetworkDataValidation.validateText(longSecret, 10),
                () -> NetworkDataValidation.validateText(SECRET, 100, 1000),
                () -> NetworkDataValidation.validateId(longSecret),
                () -> NetworkDataValidation.validateProfileId(SECRET),
                () -> NetworkDataValidation.validateTradeId(SECRET.repeat(20)),
                () -> NetworkDataValidation.validateCode(SECRET),
                () -> NetworkDataValidation.validateBtcTxId(SECRET),
                () -> NetworkDataValidation.validateBtcAddress(SECRET.repeat(5)),
                () -> NetworkDataValidation.validateHashAsHex(SECRET),
                () -> NetworkDataValidation.validatePubKeyBase64(SECRET.repeat(40)),
                () -> NetworkDataValidation.validatePubKeyHex(SECRET.repeat(10)),
                () -> NetworkDataValidation.validateSignatureBase64(SECRET.repeat(5)),
                () -> NetworkDataValidation.validateBondUserName(SECRET.repeat(5)),
                () -> NetworkDataValidation.validateECSignature(secretBytes),
                () -> NetworkDataValidation.validateECPubKey(secretBytes));

        for (Executable validation : validations) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, validation);
            assertThat(exception.getMessage())
                    .doesNotContain(SECRET)
                    .doesNotContain("DE89370400440532013000")
                    .contains("length=");
        }
    }

    @Test
    void exceptionMessageOfAByteArrayDoesNotContainItsBytes() {
        byte[] secretBytes = SECRET.getBytes();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> NetworkDataValidation.validateECSignature(secretBytes));

        // Arrays.toString of the bytes starts with the first byte of the text
        assertThat(exception.getMessage()).doesNotContain(String.valueOf(secretBytes[0]) + ",");
        assertThat(exception.getMessage()).contains("signature.length=" + secretBytes.length);
    }
}
