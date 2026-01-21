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

package bisq.api.access.client;

import bisq.api.access.permissions.Permission;
import bisq.security.keys.KeyGeneration;
import lombok.Getter;

import java.security.KeyPair;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Just for dev testing and mocks. Represents the mobile client
 */
@Getter
public abstract class Client {
    protected String qrCode;
    protected String sessionId;
    protected Set<Permission> grantedPermissions;

    public ClientIdentity createClientIdentity(String clientName) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        // TODO on Mobile client: Allow user to set device name, store ClientIdentity
        return new ClientIdentity(clientName, keyPair);
    }

    // TODO on Mobile client: implement reader
    public abstract CompletableFuture<String> readQrCode();
}
