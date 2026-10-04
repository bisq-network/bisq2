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

package bisq.bonded_roles.oracle;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;

import java.util.Set;

/**
 * Authorized data whose authorizing role type is the oracle node: oracle nodes, bonded roles, reputation data, account
 * timestamps, market prices and burningman data (see docs/specifications/bonded-roles/authorized-data.md). Its static
 * keys are the oracle node keys.
 */
public interface OracleNodeAuthorizedData extends AuthorizedDistributedData {
    @Override
    default Set<String> getAuthorizedPublicKeys() {
        return AuthorizedPubKeys.getOracleNodePubKeys();
    }

    // While only static oracle nodes are allowed, only the oracle node keys may authorize the data, with and without
    // the static flag (rule 10). So the network storage rejects the data of other keys when it adds it, and removes
    // such data when it loads a store (rules 2, 4, 5 and 16 of docs/specifications/bonded-roles/authorized-data.md).
    @Override
    default boolean isStaticPublicKeyRequired() {
        return AuthorizedBondedRolesService.ONLY_STATIC_ORACLE_NODES;
    }
}
