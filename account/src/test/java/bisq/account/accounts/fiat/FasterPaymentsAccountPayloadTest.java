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

package bisq.account.accounts.fiat;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.validation.NetworkDataValidation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The rules of AccountPayload apply to Faster Payments accounts as to all other accounts
class FasterPaymentsAccountPayloadTest {
    private static final byte[] SALT = new byte[32];
    private static final String TOO_LONG_ID = "a".repeat(NetworkDataValidation.MAX_ID_LENGTH + 1);

    @Test
    void payloadWithValidIdCanBeCreated() {
        assertDoesNotThrow(() -> payload("id"));
    }

    @Test
    void payloadWithTooLongIdCannotBeCreated() {
        assertThrows(IllegalArgumentException.class, () -> payload(TOO_LONG_ID));
    }

    @Test
    void receivedPayloadWithTooLongIdFailsAtParsing() {
        bisq.account.protobuf.AccountPayload proto = payload("id").toProto(false).toBuilder()
                .setId(TOO_LONG_ID)
                .build();

        assertThrows(UnresolvableProtobufException.class, () -> FasterPaymentsAccountPayload.fromProto(proto));
    }

    private static FasterPaymentsAccountPayload payload(String id) {
        return new FasterPaymentsAccountPayload(id, SALT, "Alice", "123456", "12345678");
    }
}
