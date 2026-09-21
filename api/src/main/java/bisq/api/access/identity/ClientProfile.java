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

package bisq.api.access.identity;

import bisq.common.proto.PersistableProto;
import bisq.security.DigestUtil;
import com.google.protobuf.ByteString;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A paired API client as the node stores it.
 * <p>
 * Holds a SHA-256 of the client secret, never the secret itself, so reading the persistence store
 * does not yield a credential that opens sessions. The secret has 256 bits of entropy, which is why
 * an unsalted hash is enough: salting and slow derivation defend low-entropy inputs. The plaintext
 * exists only in the pairing response that hands it to the client.
 * <p>
 * Hashed over the UTF-8 bytes of the secret exactly as the client presents it, which is also what
 * {@link ClientManagementId} keys its HMAC with, so management IDs did not move when hashing was
 * introduced.
 */
@Getter
@EqualsAndHashCode
public final class ClientProfile implements PersistableProto {
    private static final int HASH_LENGTH_IN_BYTES = 32;

    private static byte[] hashSecret(String clientSecret) {
        return DigestUtil.sha256(clientSecret.getBytes(StandardCharsets.UTF_8));
    }

    public static ClientProfile fromSecret(String clientId, String clientSecret, String clientName) {
        return new ClientProfile(clientId, hashSecret(clientSecret), clientName);
    }

    private final String clientId;
    @Getter(AccessLevel.NONE)
    private final byte[] clientSecretHash;
    private final String clientName;

    private ClientProfile(String clientId, byte[] clientSecretHash, String clientName) {
        checkArgument(clientSecretHash.length == HASH_LENGTH_IN_BYTES, "Client secret hash must be SHA-256 output");
        this.clientId = clientId;
        this.clientSecretHash = clientSecretHash;
        this.clientName = clientName;
    }

    /** A copy, so no caller can alter what the next session request is compared against. */
    public byte[] getClientSecretHash() {
        return clientSecretHash.clone();
    }

    /** Compared in constant time, as this is the authentication step of a session request. */
    public boolean matchesSecret(String clientSecret) {
        return MessageDigest.isEqual(hashSecret(clientSecret), clientSecretHash);
    }

    @Override
    public bisq.api.protobuf.ClientProfile toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.api.protobuf.ClientProfile.Builder getBuilder(boolean serializeForHash) {
        return bisq.api.protobuf.ClientProfile.newBuilder()
                .setClientId(clientId)
                .setClientSecretHash(ByteString.copyFrom(clientSecretHash))
                .setClientName(clientName);
    }

    public static ClientProfile fromProto(bisq.api.protobuf.ClientProfile proto) {
        return new ClientProfile(proto.getClientId(),
                hasLegacyPlaintextSecret(proto)
                        ? hashSecret(proto.getClientSecret())
                        : proto.getClientSecretHash().toByteArray(),
                proto.getClientName());
    }

    /**
     * Whether the entry still carries the plaintext secret that stores written before hashing hold.
     * The hash takes precedence if both are set, so a legacy field that an older node kept through
     * a downgrade and upgrade cycle cannot override a hash written since.
     */
    public static boolean hasLegacyPlaintextSecret(bisq.api.protobuf.ClientProfile proto) {
        return proto.getClientSecretHash().isEmpty() && !proto.getClientSecret().isEmpty();
    }

    /**
     * Whether {@link #fromProto} can build a profile that some secret could match: a legacy
     * plaintext, or a hash of the right length. Callers check this before loading an entry.
     */
    public static boolean hasUsableCredential(bisq.api.protobuf.ClientProfile proto) {
        return hasLegacyPlaintextSecret(proto) || proto.getClientSecretHash().size() == HASH_LENGTH_IN_BYTES;
    }
}
