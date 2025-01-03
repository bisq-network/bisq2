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

package bisq.security.keys;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.util.Base64;

public class JsonSerialization {
    public static class KeyPair {
        public static class Deserializer extends JsonDeserializer<java.security.KeyPair> {
            @Override
            public java.security.KeyPair deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                var node = parser.getCodec().readTree(parser);
                try {
                    JsonNode privateKeyJsonNode = (JsonNode) node.get("privateKey");
                    JsonNode publicKeyJsonNode = (JsonNode) node.get("publicKey");
                    String privateKeyEncoded = privateKeyJsonNode.asText();
                    String publicKeyEncoded = publicKeyJsonNode.asText();
                    java.security.PrivateKey privateKey = KeyGeneration.generatePrivate(Base64.getDecoder().decode(privateKeyEncoded));
                    java.security.PublicKey publicKey = KeyGeneration.generatePublic(Base64.getDecoder().decode(publicKeyEncoded));
                    return new java.security.KeyPair(publicKey, privateKey);
                } catch (Exception e) {
                    throw new IOException("Failed to deserialize KeyPair", e);
                }
            }
        }

        public static class Serializer extends JsonSerializer<java.security.KeyPair> {
            @Override
            public void serialize(java.security.KeyPair keyPair,
                                  JsonGenerator generator,
                                  SerializerProvider serializers) throws IOException {
                generator.writeStartObject();
                generator.writeStringField("privateKey", Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
                generator.writeStringField("publicKey", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
                generator.writeEndObject();
            }
        }
    }

    public static class PrivateKey {
        public static class Deserializer extends JsonDeserializer<java.security.PrivateKey> {
            @Override
            public java.security.PrivateKey deserialize(JsonParser parser,
                                                        DeserializationContext context) throws IOException {
                try {
                    byte[] bytes = Base64.getDecoder().decode(parser.getText());
                    return KeyGeneration.generatePrivate(bytes);
                } catch (Exception e) {
                    throw new IOException("Failed to deserialize publicKey", e);
                }
            }
        }

        public static class Serializer extends JsonSerializer<java.security.PrivateKey> {
            @Override
            public void serialize(java.security.PrivateKey value,
                                  JsonGenerator generator,
                                  SerializerProvider serializers) throws IOException {
                generator.writeString(Base64.getEncoder().encodeToString(value.getEncoded()));
            }
        }
    }

    public static class PublicKey {
        public static class Deserializer extends JsonDeserializer<java.security.PublicKey> {
            @Override
            public java.security.PublicKey deserialize(JsonParser parser,
                                                       DeserializationContext context) throws IOException {
                try {
                    byte[] bytes = Base64.getDecoder().decode(parser.getText());
                    return KeyGeneration.generatePublic(bytes);
                } catch (Exception e) {
                    throw new IOException("Failed to deserialize publicKey", e);
                }
            }
        }

        public static class Serializer extends JsonSerializer<java.security.PublicKey> {
            @Override
            public void serialize(java.security.PublicKey value,
                                  JsonGenerator generator,
                                  SerializerProvider serializers) throws IOException {
                generator.writeString(Base64.getEncoder().encodeToString(value.getEncoded()));
            }
        }
    }
}
