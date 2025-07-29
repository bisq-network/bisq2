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
@Getter
@EqualsAndHashCode
@ToString
public class I2PKeyPair implements PersistableProto {
    private final byte[] destination;

    public I2PKeyPair(byte[] destination) {
        if (destination == null) {
            throw new IllegalArgumentException("destination must not be null");
        }
        this.destination = destination;
    }

    @Override
    public Message.Builder getBuilder(boolean serializeForHash) {
        return bisq.security.protobuf.I2PKeyPair.newBuilder().setDestinationKey(ByteString.copyFrom(destination));
    }

    @Override
    public bisq.security.protobuf.I2PKeyPair toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static I2PKeyPair fromProto(bisq.security.protobuf.I2PKeyPair proto) {
        byte[] destBytes = proto.getDestinationKey().toByteArray();
        if (destBytes.length == 0) {
            throw new IllegalArgumentException("Missing destinationKey bytes");
        }
        Destination dest;
        try (ByteArrayInputStream in = new ByteArrayInputStream(destBytes)) {
            dest = Destination.create(in);
        } catch (IOException | DataFormatException e) {
            throw new IllegalStateException("Failed to deserialize destinationKey", e);
        }
        return new I2PKeyPair(destBytes);
    }

    public String getBase32Address() {
        Destination dest;
        try (ByteArrayInputStream in = new ByteArrayInputStream(destination)) {
            dest = Destination.create(in);
        } catch (IOException | DataFormatException e) {
            throw new IllegalStateException("Failed to deserialize destinationKey", e);
        }
        return dest.toBase32();
    }

    public String getBase64Destination() {
        Destination dest;
        try (ByteArrayInputStream in = new ByteArrayInputStream(destination)) {
            dest = Destination.create(in);
        } catch (IOException | DataFormatException e) {
            throw new IllegalStateException("Failed to deserialize destinationKey", e);
        }
        return dest.toBase64();
    }
}
