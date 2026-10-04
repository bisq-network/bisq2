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

/**
 * Temporary restriction: while it is enabled, only the static oracle node keys may act as an oracle node, and the
 * network storage accepts the data which oracle nodes authorize only from these keys (rules 10 to 14 and 16 of
 * docs/specifications/bonded-roles/authorized-data.md). The configuration application.bondedRoles.onlyStaticOracleNodes
 * sets it when the application starts (see BondedRolesService). All nodes of a network must use the same value.
 * <p>
 * The value is global for the node, because the network storage reads it through the type of the data
 * (OracleNodeAuthorizedData.isStaticPublicKeyRequired()). It is set before the stores load their data, and it does not
 * change while the node runs, because the stores check it when they add data and when they load.
 */
public final class OnlyStaticOracleNodes {
    private static volatile boolean enabled = true;

    private OnlyStaticOracleNodes() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        OnlyStaticOracleNodes.enabled = enabled;
    }
}
