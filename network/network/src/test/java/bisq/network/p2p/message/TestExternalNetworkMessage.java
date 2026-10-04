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

package bisq.network.p2p.message;

import bisq.common.proto.UnresolvableProtobufException;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A message of another module, like the messages of the chat, trade and support modules. The network module has no
 * such message, so its tests use this one. The network layer carries it as ExternalNetworkMessage with a StringValue
 * in the Any field.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class TestExternalNetworkMessage implements ExternalNetworkMessage {
    private static final String PROTO_TYPE = "google.protobuf.StringValue";

    // Without a resolver, the node cannot parse the message at all, and a test could not show which rule drops it
    public static void registerResolver() {
        NetworkMessageResolver.addResolver(PROTO_TYPE, TestExternalNetworkMessage.class, TestExternalNetworkMessage::fromAny);
    }

    private final String text;

    public TestExternalNetworkMessage(String text) {
        this.text = text;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public StringValue.Builder getValueBuilder(boolean serializeForHash) {
        return StringValue.newBuilder().setValue(text);
    }

    private static TestExternalNetworkMessage fromAny(Any any) throws UnresolvableProtobufException {
        try {
            return new TestExternalNetworkMessage(any.unpack(StringValue.class).getValue());
        } catch (InvalidProtocolBufferException e) {
            throw new UnresolvableProtobufException(e);
        }
    }
}
