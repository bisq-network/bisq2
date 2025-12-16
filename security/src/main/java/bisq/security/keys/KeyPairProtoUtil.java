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
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * We use java KeyPair class for persistence.
 * This class adds support for protobuf serialisation.
 */
@Slf4j
public class KeyPairProtoUtil {
    public static bisq.security.protobuf.KeyPair toProto(java.security.KeyPair keyPair) {
        return bisq.security.protobuf.KeyPair.newBuilder()
                .setPrivateKey(ByteString.copyFrom(keyPair.getPrivate().getEncoded()))
                .setPublicKey(ByteString.copyFrom(keyPair.getPublic().getEncoded()))
                .build();
    }

    public static java.security.KeyPair fromProto(bisq.security.protobuf.KeyPair keyPair) {
        return fromProto(keyPair, KeyGeneration.ECDH);
    }

    public static java.security.KeyPair fromProto(bisq.security.protobuf.KeyPair keyPair, String keyAlgorithm) {
        try {
            PrivateKey privateKey = KeyGeneration.generatePrivate(keyPair.getPrivateKey().toByteArray(), keyAlgorithm);
            PublicKey publicKey = KeyGeneration.generatePublic(keyPair.getPublicKey().toByteArray(), keyAlgorithm);
            return new java.security.KeyPair(publicKey, privateKey);
        } catch (GeneralSecurityException e) {
            log.error("fromProto failed", e);
            throw new RuntimeException(e);
        }
    }
}