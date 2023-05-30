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
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>An instance of EncryptedData is a holder for an initialization vector and encrypted bytes. It is typically
 * used to hold encrypted private key bytes.</p>
 *
 * <p>The initialisation vector is random data that is used to initialise the AES block cipher when the
 * private key bytes were encrypted. You need these for decryption.</p>
 */
@Getter
@Slf4j
@EqualsAndHashCode
public final class EncryptedData implements Proto {
    private final byte[] initialisationVector;
    private final byte[] encryptedBytes;

    public EncryptedData(byte[] initialisationVector, byte[] encryptedBytes) {
        this.initialisationVector = initialisationVector;
        this.encryptedBytes = encryptedBytes;
    }

    public bisq.security.protobuf.EncryptedData toProto() {
        return bisq.security.protobuf.EncryptedData.newBuilder()
                .setInitialisationVector(ByteString.copyFrom(initialisationVector))
                .setEncryptedBytes(ByteString.copyFrom(encryptedBytes))
                .build();
    }

    public static EncryptedData fromProto(bisq.security.protobuf.EncryptedData proto) {
        return new EncryptedData(proto.getInitialisationVector().toByteArray(), proto.getEncryptedBytes().toByteArray());
    }

    @Override
    public String toString() {
        return "EncryptedData{" +
                "\r\n     initialisationVector=" + Hex.encode(initialisationVector) +
                ",\r\n     encryptedBytes=" + Hex.encode(encryptedBytes) +
                "\r\n}";
    }
}