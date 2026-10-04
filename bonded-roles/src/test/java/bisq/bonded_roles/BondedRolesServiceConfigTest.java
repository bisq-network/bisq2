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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A node which no configuration sets uses true, for example the node app of bisq-mobile, which builds the configuration
 * from its own file (see docs/specifications/bonded-roles/authorized-data.md).
 */
class BondedRolesServiceConfigTest {
    private static final String OTHER_KEYS = """
            marketPrice {}
            blockchainExplorer {}
            mobileNotifications {}
            ignoreSecurityManager = false
            """;

    @Test
    void missingOnlyStaticOracleNodesIsTrue() {
        assertTrue(BondedRolesService.Config.from(config("")).isOnlyStaticOracleNodes());
    }

    @Test
    void configuredOnlyStaticOracleNodesIsUsed() {
        assertFalse(BondedRolesService.Config.from(config("onlyStaticOracleNodes = false")).isOnlyStaticOracleNodes());
        assertTrue(BondedRolesService.Config.from(config("onlyStaticOracleNodes = true")).isOnlyStaticOracleNodes());
    }

    private static Config config(String onlyStaticOracleNodes) {
        return ConfigFactory.parseString(OTHER_KEYS + onlyStaticOracleNodes);
    }
}
