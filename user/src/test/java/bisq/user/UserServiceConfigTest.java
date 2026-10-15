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

package bisq.user;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceConfigTest {

    @Test
    void fromReadsEnabledRateLimitConfig() {
        UserService.Config config = UserService.Config.from(ConfigFactory.parseString("rateLimitEnabled=true"));

        assertTrue(config.isRateLimitEnabled());
    }

    @Test
    void fromReadsDisabledRateLimitConfig() {
        UserService.Config config = UserService.Config.from(ConfigFactory.parseString("rateLimitEnabled=false"));

        assertFalse(config.isRateLimitEnabled());
    }

    @Test
    void fromDefaultsRateLimitEnabledWhenKeyIsMissing() {
        UserService.Config config = UserService.Config.from(ConfigFactory.empty());

        assertTrue(config.isRateLimitEnabled());
    }
}
