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

package bisq.api.access;

import bisq.api.access.identity.ClientProfile;
import bisq.api.access.pairing.PairingCode;
import bisq.api.access.permissions.Permission;
import lombok.Getter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class MockApiAccessStoreService extends ApiAccessStoreService {
    private final Map<String, PairingCode> pairingCodeByIdMap = new ConcurrentHashMap<>();
    private final Map<String, ClientProfile> clientProfileByIdMap = new ConcurrentHashMap<>();
    private final Map<String, Set<Permission>> permissionsByClientId = new ConcurrentHashMap<>();

    public MockApiAccessStoreService() {
        super();
    }

    @Override
    public void removePairingCode(String pairingCodeId, PairingCode pairingCode) {
        pairingCodeByIdMap.remove(pairingCodeId, pairingCode);
        persist();
    }

    @Override
    public void putClientProfile(String clientId, ClientProfile clientProfile) {
        clientProfileByIdMap.put(clientId, clientProfile);
    }

    @Override
    public void putPairingCode(String id, PairingCode pairingCode) {
        pairingCodeByIdMap.put(id, pairingCode);
    }

    @Override
    public void putPermissions(String clientId, Set<Permission> permissions) {
        permissionsByClientId.put(clientId, permissions);
    }
}
