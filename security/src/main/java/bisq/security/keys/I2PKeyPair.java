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

import bisq.common.proto.PersistableProto;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@EqualsAndHashCode
@ToString
public class I2PKeyPair implements PersistableProto {
    // fullKeyMaterial only contains private and public key material
    @Getter
    private final byte[] identityBytes;

    // Destination only contains public key material
    @Getter
    private final byte[] destinationBytes;

    private transient Destination destination;
    private transient String destinationBase32;
    private transient String destinationBase64;

    public I2PKeyPair(byte[] identityBytes, Destination destination) {
        this.identityBytes = identityBytes;
        this.destination = destination;
        this.destinationBytes = destination.toByteArray();
    }

    public I2PKeyPair(byte[] identityBytes, byte[] destinationBytes) {
        this.identityBytes = identityBytes;
        this.destinationBytes = destinationBytes;
    }

    @Override
    public Message.Builder getBuilder(boolean serializeForHash) {
        return bisq.security.protobuf.I2PKeyPair.newBuilder()
                .setIdentityBytes(ByteString.copyFrom(identityBytes))
                .setDestinationBytes(ByteString.copyFrom(destinationBytes));
    }

    @Override
    public bisq.security.protobuf.I2PKeyPair toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static I2PKeyPair fromProto(bisq.security.protobuf.I2PKeyPair proto) {
        return new I2PKeyPair(proto.getIdentityBytes().toByteArray(),
                proto.getDestinationBytes().toByteArray());
    }

    public Destination getDestination() {
        if (destination == null) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(destinationBytes)) {
                destination = Destination.create(inputStream);
            } catch (IOException | DataFormatException e) {
                throw new IllegalStateException("Failed to deserialize destinationKey", e);
            }
        }
        return destination;
    }

    public String getDestinationBase32() {
        if (destinationBase32 == null) {
            destinationBase32 = getDestination().toBase32();
        }
        return destinationBase32;
    }

    public String getDestinationBase64() {
        if (destinationBase64 == null) {
            destinationBase64 = getDestination().toBase64();
        }
        return destinationBase64;
    }
}
