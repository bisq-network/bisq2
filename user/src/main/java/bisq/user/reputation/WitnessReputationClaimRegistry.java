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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.user.reputation;

import bisq.common.data.ByteArray;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class WitnessReputationClaimRegistry {
    interface Listener {
        void onClaimsChanged(Set<String> affectedProfileIds);
    }

    private record Claim(ByteArray oraclePublicKey,
                         AuthorizedDistributedData data,
                         String profileId,
                         ByteArray witnessNullifier) {
    }

    private final Set<Claim> claims = new HashSet<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    void addListener(Listener listener) {
        listeners.add(listener);
    }

    void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    void add(AuthorizedData authorizedData, String profileId, byte[] witnessNullifier) {
        update(new Claim(new ByteArray(authorizedData.getAuthorizedPublicKeyBytes()),
                authorizedData.getAuthorizedDistributedData(),
                profileId,
                new ByteArray(witnessNullifier)), true);
    }

    void remove(AuthorizedData authorizedData, String profileId, byte[] witnessNullifier) {
        update(new Claim(new ByteArray(authorizedData.getAuthorizedPublicKeyBytes()),
                authorizedData.getAuthorizedDistributedData(),
                profileId,
                new ByteArray(witnessNullifier)), false);
    }

    synchronized Set<String> getClaimingProfileIds(byte[] witnessNullifier) {
        ByteArray key = new ByteArray(witnessNullifier);
        Set<String> profileIds = new HashSet<>();
        claims.stream()
                .filter(claim -> claim.witnessNullifier().equals(key))
                .map(Claim::profileId)
                .forEach(profileIds::add);
        return Set.copyOf(profileIds);
    }

    boolean hasConflict(byte[] witnessNullifier) {
        return getClaimingProfileIds(witnessNullifier).size() > 1;
    }

    private void update(Claim claim, boolean add) {
        Set<String> affectedProfileIds;
        boolean changed;
        synchronized (this) {
            affectedProfileIds = new HashSet<>(getClaimingProfileIds(claim.witnessNullifier().getBytes()));
            changed = add ? claims.add(claim) : claims.remove(claim);
            if (!changed) {
                return;
            }
            affectedProfileIds.add(claim.profileId());
            affectedProfileIds.addAll(getClaimingProfileIds(claim.witnessNullifier().getBytes()));
        }
        Set<String> immutableProfileIds = Set.copyOf(affectedProfileIds);
        listeners.forEach(listener -> listener.onClaimsChanged(immutableProfileIds));
    }
}
