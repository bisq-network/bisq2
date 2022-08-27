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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class ConfidentialData implements Proto {
    private final byte[] senderPublicKey;
    private final byte[] hmac;
    private final byte[] iv;
    private final byte[] cypherText;
    private final byte[] signature;

    public ConfidentialData(byte[] senderPublicKey,
                            byte[] hmac,
                            byte[] iv,
                            byte[] cypherText,
                            byte[] signature) {
        this.senderPublicKey = senderPublicKey;
        this.hmac = hmac;
        this.iv = iv;
        this.cypherText = cypherText;
        this.signature = signature;
    }

    public bisq.security.protobuf.ConfidentialData toProto() {
        return bisq.security.protobuf.ConfidentialData.newBuilder()
                .setSenderPublicKey(ByteString.copyFrom(senderPublicKey))
                .setHmac(ByteString.copyFrom(hmac))
                .setIv(ByteString.copyFrom(iv))
                .setCypherText(ByteString.copyFrom(cypherText))
                .setSignature(ByteString.copyFrom(signature))
                .build();
    }

    public static ConfidentialData fromProto(bisq.security.protobuf.ConfidentialData proto) {
        return new ConfidentialData(proto.getSenderPublicKey().toByteArray(),
                proto.getHmac().toByteArray(),
                proto.getIv().toByteArray(),
                proto.getCypherText().toByteArray(),
                proto.getSignature().toByteArray());
    }
}
