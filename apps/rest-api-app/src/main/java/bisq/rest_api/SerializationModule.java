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

package bisq.rest_api;

import bisq.security.keys.KeyPairJsonSer;
import bisq.security.keys.PrivateKeyJsonSer;
import bisq.security.keys.PublicKeyJsonSer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class SerializationModule extends SimpleModule {
    public SerializationModule() {
        addSerializer(KeyPair.class, new KeyPairJsonSer.Serializer());
        addSerializer(PublicKey.class, new PublicKeyJsonSer.Serializer());
        addDeserializer(PublicKey.class, new PublicKeyJsonSer.Deserializer());
        addSerializer(PrivateKey.class, new PrivateKeyJsonSer.Serializer());
        addDeserializer(PrivateKey.class, new PrivateKeyJsonSer.Deserializer());
    }
}
