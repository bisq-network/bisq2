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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class KeyPairJsonSer {
    public static class Deserializer extends JsonDeserializer<java.security.KeyPair> {
        @Override
        public KeyPair deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            var node = parser.getCodec().readTree(parser);
            try {
                String privateKeyEncoded = node.get("privateKey").toString();
                String publicKeyEncoded = node.get("publicKey").toString();
                PrivateKey privateKey = KeyGeneration.generatePrivate(Base64.getDecoder().decode(privateKeyEncoded));
                PublicKey publicKey = KeyGeneration.generatePublic(Base64.getDecoder().decode(publicKeyEncoded));
                return new KeyPair(publicKey, privateKey);
            } catch (Exception e) {
                throw new IOException("Failed to deserialize KeyPair", e);
            }
        }
    }

    public static class Serializer extends JsonSerializer<java.security.KeyPair> {
        @Override
        public void serialize(KeyPair keyPair,
                              JsonGenerator generator,
                              SerializerProvider serializers) throws IOException {
            generator.writeStartObject();
            generator.writeStringField("privateKey", Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            generator.writeStringField("publicKey", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            generator.writeEndObject();
        }
    }
}
