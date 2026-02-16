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

package bisq.http_api.access.identity;

import bisq.common.proto.PersistableProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Represents a client profile for API access pairing.
 *
 * <p><b>Security Note:</b> The clientSecret is currently stored in plaintext and serialized via protobuf.
 * This creates exposure risk if the persistence store is compromised.
 * Future enhancement should implement encryption for the persistence layer and document/enforce
 * strict file-level protections and rotation policies.
 *
 * <p><b>Authentication:</b> Constant-time secret comparison is performed at authentication using
 * {@link java.security.MessageDigest#isEqual(byte[], byte[])} in
 * {@link bisq.http_api.access.ApiAccessService#requestSession(String, String)}.
 * See that method for the secure comparison implementation.
 */
@Getter
@EqualsAndHashCode
public class ClientProfile implements PersistableProto {
    private final String clientId;
    private final String clientSecret;
    private final String clientName;

    public ClientProfile(String clientId, String clientSecret, String clientName) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clientName = clientName;
    }

    @Override
    public bisq.http_api.protobuf.ClientProfile toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.http_api.protobuf.ClientProfile.Builder getBuilder(boolean serializeForHash) {
        return bisq.http_api.protobuf.ClientProfile.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setClientName(clientName);
    }

    public static ClientProfile fromProto(bisq.http_api.protobuf.ClientProfile proto) {
        return new ClientProfile(proto.getClientId(),
                proto.getClientSecret(),
                proto.getClientName());
    }
}
