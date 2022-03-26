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

package bisq.security;

import bisq.common.proto.Proto;
import com.google.protobuf.ByteString;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

public record PubKey(PublicKey publicKey, String keyId) implements Proto {
    public bisq.security.protobuf.PubKey toProto() {
        return bisq.security.protobuf.PubKey.newBuilder()
                .setPublicKey(ByteString.copyFrom(publicKey.getEncoded()))
                .setKeyId(keyId)
                .build();
    }

    public static PubKey fromProto(bisq.security.protobuf.PubKey proto) {
        try {
            PublicKey publicKey = KeyGeneration.generatePublic(proto.getPublicKey().toByteArray());
            return new PubKey(publicKey, proto.getKeyId());
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}