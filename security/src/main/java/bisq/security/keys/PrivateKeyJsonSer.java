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
import java.util.Base64;

public class PrivateKeyJsonSer {
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
