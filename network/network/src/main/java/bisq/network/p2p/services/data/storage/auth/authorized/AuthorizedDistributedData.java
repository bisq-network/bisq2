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

package bisq.network.p2p.services.data.storage.auth.authorized;

import bisq.network.p2p.services.data.storage.DistributedData;

import java.util.HashSet;
import java.util.Set;

public interface AuthorizedDistributedData extends DistributedData {
    default Set<String> getAuthorizedPublicKeys() {
        return new HashSet<>();
    }

    default boolean staticPublicKeysProvided() {
        return false;
    }

    // Oracle and seed nodes of earlier versions published data of some types with the key pair of their network
    // identity instead of their authorized key pair. For these types, data created before
    // AuthorizedData.LEGACY_OWNER_CUTOFF_DATE can have another owner than its authorized key. Can be removed when no
    // such data is valid anymore (see docs/specifications/network/storage-ownership.md).
    default boolean isLegacyOwnerAccepted() {
        return false;
    }
}