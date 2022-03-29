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

package bisq.network.p2p;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestProtobufAny {

   /* private static class MockOffer {
        private final String text;

        MockOffer(String text) {
            this.text = text;
        }

        public static bisq.network.protobuf.MockOffer unpack(Any any)
                throws InvalidProtocolBufferException {
            return any.unpack( bisq.network.protobuf.MockOffer.class);
        }

        public static MockOffer fromProto(bisq.network.protobuf.MockOffer proto) {
            return new MockOffer(proto.getText());
        }

      *//*  public static Class<bisq.network.protobuf.MockOffer> getProtoClazz() {
            return bisq.network.protobuf.MockOffer.class;
        }*//*


        public bisq.network.protobuf.MockOffer toProto() {
            return bisq.network.protobuf.MockOffer.newBuilder().setText(text).build();
        }

        public Any toAnyProto() {
            return Any.pack(toProto());
        }
    }*/

   /* private static class MockChatMessage {
        private final String message;

        MockChatMessage(String message) {
            this.message = message;
        }

        public static MockChatMessage fromProto(bisq.network.protobuf.MockChatMessage proto) {
            return new MockChatMessage(proto.getMessage());
        }

        public bisq.network.protobuf.MockChatMessage toProto() {
            return bisq.network.protobuf.MockChatMessage.newBuilder().setMessage(message).build();
        }
    }*/

    @Test
    public void testProtobufAny() {
       /* MockOffer mockOffer = new MockOffer("test1");
        //  MockChatMessage mockChatMessage = new MockChatMessage("test2");
        bisq.network.protobuf.MockOffer mockOfferProto = mockOffer.toProto();

        Any mockOfferProtoAsAny = Any.pack(mockOfferProto);
        String type = mockOfferProtoAsAny.getTypeUrl();
        log.error("type {}", type);
        try {
            bisq.network.protobuf.MockOffer unpacked = mockOfferProtoAsAny.unpack(bisq.network.protobuf.MockOffer.class);
            MockOffer resolved = MockOffer.fromProto(unpacked);
            assertEquals(mockOffer.text, resolved.text);

            String clazzNameWithProtoPackage = mockOfferProtoAsAny.getTypeUrl().split("/")[1];
            String protoPackage = clazzNameWithProtoPackage.split("\\.")[0];
            String clazzName = clazzNameWithProtoPackage.split("\\.")[1];
            String javaFQN = "bisq." + protoPackage + ".protobuf." + clazzName;
            
            log.error("clazzNameWithProtoPackage {}", clazzNameWithProtoPackage);
            log.error("protoPackage {}", protoPackage);
            log.error("clazzName {}", clazzName);
            log.error("javaFQN {}", javaFQN);
            // Class<bisq.network.protobuf.MockOffer> clazz1 = (Class<bisq.network.protobuf.MockOffer>) Class.forName(javaFQN);
            Class<com.google.protobuf.Any> clazz = (Class<com.google.protobuf.Any>) Class.forName(javaFQN);

            log.error("clazz {}", clazz);

            bisq.network.protobuf.MockOffer unpacked2 = MockOffer.unpack(mockOfferProtoAsAny);
            MockOffer resolvedWithReflection = MockOffer.fromProto(unpacked2);
            assertEquals(mockOffer.text, resolvedWithReflection.text);
        } catch (ClassNotFoundException | InvalidProtocolBufferException e) {
            e.printStackTrace();
        }*/
    }


  /*  public Any toProto() {
        Ping ping = new Ping(1);
        NetworkMessage proto = ping.toProto();
        return Any.pack(proto);
    }*/
}