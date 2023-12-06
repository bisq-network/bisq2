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

import com.google.protobuf.ByteString;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * We use java KeyPair class for persistence.
 * This class adds support for protobuf serialisation.
 */
public class KeyPairProtoUtil {
    public static bisq.security.protobuf.KeyPair toProto(java.security.KeyPair keyPair) {
        return bisq.security.protobuf.KeyPair.newBuilder()
                .setPrivateKey(ByteString.copyFrom(keyPair.getPrivate().getEncoded()))
                .setPublicKey(ByteString.copyFrom(keyPair.getPublic().getEncoded()))
                .build();
    }

    public static java.security.KeyPair fromProto(bisq.security.protobuf.KeyPair keyPair) {
        try {
            PrivateKey privateKey = KeyGeneration.generatePrivate(keyPair.getPrivateKey().toByteArray());
            PublicKey publicKey = KeyGeneration.generatePublic(keyPair.getPublicKey().toByteArray());
            return new java.security.KeyPair(publicKey, privateKey);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}