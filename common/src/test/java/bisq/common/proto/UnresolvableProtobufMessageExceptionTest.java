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

package bisq.common.proto;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnresolvableProtobufMessageExceptionTest {
    private static final String SECRET = "privateKeyBytesWhichMustNotBeLogged";
    private static final StringValue PROTO = StringValue.newBuilder().setValue(SECRET).build();
    private static final Any ANY = Any.pack(PROTO);

    @Test
    void noConstructorPutsTheProtoContentIntoTheMessage() {
        List<UnresolvableProtobufMessageException> exceptions = List.of(
                new UnresolvableProtobufMessageException(PROTO),
                new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", PROTO),
                new UnresolvableProtobufMessageException(ANY),
                new UnresolvableProtobufMessageException(ANY, new RuntimeException("cause")));

        exceptions.forEach(exception ->
                assertFalse(exception.getMessage().contains(SECRET), exception.getMessage()));
    }

    @Test
    void theMessageStillIdentifiesTheProtoType() {
        assertTrue(new UnresolvableProtobufMessageException(PROTO).getMessage()
                .contains("google.protobuf.StringValue"));
        assertTrue(new UnresolvableProtobufMessageException(ANY).getMessage()
                .contains("type.googleapis.com/google.protobuf.StringValue"));
    }

    @Test
    void conversionFailuresDoNotRetainPartialMessagesInTheirCauses() {
        // A valid Any payload ending in an invalid tag causes unpack() to retain a partial StringValue.
        Any malformed = ANY.toBuilder()
                .setValue(ANY.getValue().concat(ByteString.copyFrom(new byte[]{0})))
                .build();
        List<Function<Throwable, UnresolvableProtobufException>> constructors = List.of(
                UnresolvableProtobufException::new,
                cause -> new UnresolvableProtobufException("conversion failed", cause),
                cause -> new UnresolvableProtobufMessageException(malformed, cause));

        for (var constructor : constructors) {
            InvalidProtocolBufferException parseFailure = assertThrows(InvalidProtocolBufferException.class,
                    () -> malformed.unpack(StringValue.class));
            RuntimeException cause = new RuntimeException("resolver failed", parseFailure);

            UnresolvableProtobufException exception = constructor.apply(cause);

            assertSame(cause, exception.getCause());
            assertSame(parseFailure, exception.getCause().getCause());
            assertNull(parseFailure.getUnfinishedMessage());
            assertFalse(parseFailure.getMessage().isBlank());
        }
    }
}
