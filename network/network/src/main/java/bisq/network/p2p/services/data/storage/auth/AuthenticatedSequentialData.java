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

package bisq.network.p2p.services.data.storage.auth;

import bisq.common.encoding.Hex;
import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Data which ensures that the sequence of add or remove request is maintained correctly.
 * The data gets hashed and signed and need to be deterministic.
 */
@Slf4j
@Getter
@EqualsAndHashCode
public final class AuthenticatedSequentialData implements NetworkProto {
    public static AuthenticatedSequentialData from(AuthenticatedSequentialData data, int sequenceNumber) {
        return new AuthenticatedSequentialData(data.getAuthenticatedData(),
                sequenceNumber,
                data.getPubKeyHash(),
                data.getCreated());
    }

    private final AuthenticatedData authenticatedData;
    private final int sequenceNumber;
    private final long created;
    private final byte[] pubKeyHash;

    public AuthenticatedSequentialData(AuthenticatedData authenticatedData,
                                       int sequenceNumber,
                                       byte[] pubKeyHash,
                                       long created) {
        this.authenticatedData = authenticatedData;
        this.sequenceNumber = sequenceNumber;
        this.pubKeyHash = pubKeyHash;
        this.created = created;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(pubKeyHash);
        NetworkDataValidation.validateDate(created);
    }

    @Override
    public bisq.network.protobuf.AuthenticatedSequentialData toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    @Override
    public bisq.network.protobuf.AuthenticatedSequentialData.Builder getBuilder(boolean ignoreAnnotation) {
        return bisq.network.protobuf.AuthenticatedSequentialData.newBuilder()
                .setAuthenticatedData(authenticatedData.toProto(ignoreAnnotation))
                .setSequenceNumber(sequenceNumber)
                .setPubKeyHash(ByteString.copyFrom(pubKeyHash))
                .setCreated(created);
    }

    public static AuthenticatedSequentialData fromProto(bisq.network.protobuf.AuthenticatedSequentialData proto) {
        return new AuthenticatedSequentialData(AuthenticatedData.fromProto(proto.getAuthenticatedData()),
                proto.getSequenceNumber(),
                proto.getPubKeyHash().toByteArray(),
                proto.getCreated());
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - created) > authenticatedData.getMetaData().getTtl();
    }

    public boolean isSequenceNrInvalid(long seqNumberFromMap) {
        return sequenceNumber <= seqNumberFromMap;
    }

    @Override
    public String toString() {
        return "AuthenticatedSequentialData{" +
                "\r\n          sequenceNumber=" + sequenceNumber +
                ",\r\n          created=" + created +
                ",\r\n          pubKeyHash=" + Hex.encode(pubKeyHash) +
                ",\r\n          authenticatedData=" + authenticatedData +
                "\r\n}";
    }
}
