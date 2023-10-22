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

import bisq.common.encoding.Hex;
import bisq.common.proto.Proto;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
public final class PubKey implements Proto {
    @Getter
    private final PublicKey publicKey;
    @Getter
    private final String keyId;

    private transient byte[] hash;
    private transient String id;

    public PubKey(PublicKey publicKey, String keyId) {
        this.publicKey = publicKey;
        this.keyId = keyId;

        NetworkDataValidation.validateId(keyId);
    }

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

    public byte[] getHash() {
        if (hash == null) {
            hash = DigestUtil.hash(publicKey.getEncoded());
        }
        return hash;
    }

    public String getId() {
        if (id == null) {
            id = Hex.encode(getHash());
        }
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PubKey pubKey = (PubKey) o;

        if (!Arrays.equals(publicKey.getEncoded(), pubKey.publicKey.getEncoded())) return false;
        if (!keyId.equals(pubKey.keyId)) return false;
        if (!Arrays.equals(hash, pubKey.hash)) return false;
        return Objects.equals(id, pubKey.id);
    }

    @Override
    public int hashCode() {
        int result = publicKey != null ? Arrays.hashCode(publicKey.getEncoded()) : 0;
        result = 31 * result + keyId.hashCode();
        result = 31 * result + Arrays.hashCode(hash);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PubKey{" +
                "publicKey=" + StringUtils.truncate(publicKey.toString(), 20) +
                ", keyId='" + keyId + '\'' +
                "}";
    }
}