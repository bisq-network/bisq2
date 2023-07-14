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

package bisq.bonded_roles;

import java.util.Set;

public class AuthorizedPubKeys {
    // todo Production key not set yet - we use devMode key only yet
    public static final Set<String> KEYS = Set.of(
            // OracleNode1
            "3056301006072a8648ce3d020106052b8104000a03420004b9f698d9644d01193eaa2e7a823570aeea50e4f96749305db523c010e998b3a8f2ef0a567bb9282e80ff66b6de8f0df39d242f609728def1dbaa6f1862429188",
            // SeedNode1
            "3056301006072a8648ce3d020106052b8104000a03420004f5bfe50bd68e2f1be3011555d8870a0485000b253eece24184165012b4689db56be1c871f35c0e2d544fdf0b330ca29a0d063a89a32004e8acd640045b0d4b23"
    );
}