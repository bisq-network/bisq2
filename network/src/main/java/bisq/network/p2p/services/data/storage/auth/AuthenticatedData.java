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

import bisq.common.encoding.ObjectSerializer;
import bisq.common.encoding.Hex;
import bisq.common.encoding.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class AuthenticatedData implements Proto {

    public static AuthenticatedData from(AuthenticatedData data, int sequenceNumber) {
        return new AuthenticatedData(data.getPayload(),
                sequenceNumber,
                data.getHashOfPublicKey(),
                data.getCreated());
    }

    protected final AuthenticatedPayload payload;
    protected final int sequenceNumber;
    protected final long created;
    protected final byte[] hashOfPublicKey;

    public AuthenticatedData(AuthenticatedPayload payload,
                             int sequenceNumber,
                             byte[] hashOfPublicKey,
                             long created) {
        this.payload = payload;
        this.sequenceNumber = sequenceNumber;
        this.hashOfPublicKey = hashOfPublicKey;
        this.created = created;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - created) > payload.getMetaData().getTtl();
    }

    public boolean isSequenceNrInvalid(long seqNumberFromMap) {
        return sequenceNumber <= seqNumberFromMap;
    }

    public byte[] serialize() {
        return ObjectSerializer.serialize(this);
    }

    @Override
    public String toString() {
        return "AuthenticatedData{" +
                "\r\n     message=" + payload +
                ",\r\n     sequenceNumber=" + sequenceNumber +
                ",\r\n     created=" + created +
                ",\r\n     hashOfPublicKey=" + Hex.encode(hashOfPublicKey) +
                "\r\n}";
    }
}
