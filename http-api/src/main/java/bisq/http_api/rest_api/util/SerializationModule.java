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

package bisq.http_api.rest_api.util;

import bisq.security.keys.JsonSerialization;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class SerializationModule extends SimpleModule {
    public SerializationModule() {
        addSerializer(KeyPair.class, new JsonSerialization.KeyPair.Serializer());
        addSerializer(PublicKey.class, new JsonSerialization.PublicKey.Serializer());
        addDeserializer(PublicKey.class, new JsonSerialization.PublicKey.Deserializer());
        addSerializer(PrivateKey.class, new JsonSerialization.PrivateKey.Serializer());
        addDeserializer(PrivateKey.class, new JsonSerialization.PrivateKey.Deserializer());
    }
}
