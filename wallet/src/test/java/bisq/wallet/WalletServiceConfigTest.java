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

package bisq.wallet;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletServiceConfigTest {
    @Test
    void mockIsOffWhenTheKeyIsAbsent() {
        WalletService.Config config = WalletService.Config.from(ConfigFactory.parseString(
                "enabled=true, host=\"127.0.0.1\", port=50051"));

        assertTrue(config.isEnabled());
        assertEquals("127.0.0.1", config.getHost());
        assertEquals(50051, config.getPort());
        assertFalse(config.isMock());
    }

    @Test
    void aRemoteHostIsRefusedWhileTheConnectionIsPlaintext() {
        assertThrows(IllegalArgumentException.class, () -> WalletService.Config.from(ConfigFactory.parseString(
                "enabled=true, host=\"10.0.0.5\", port=50051")));
        assertThrows(IllegalArgumentException.class, () -> WalletService.Config.from(ConfigFactory.parseString(
                "enabled=true, host=\"wallet.example.org\", port=50051")));
    }

    @Test
    void loopbackHostsAreAccepted() {
        for (String host : new String[]{"127.0.0.1", "127.1.2.3", "localhost", "LOCALHOST", "::1", "0:0:0:0:0:0:0:1"}) {
            assertEquals(host, WalletService.Config.from(ConfigFactory.parseString(
                    "enabled=true, host=\"" + host + "\", port=50051")).getHost());
        }
    }

    @Test
    void aPaddedLoopbackHostIsAcceptedAndStoredWithoutThePadding() {
        assertEquals("127.0.0.1", WalletService.Config.from(ConfigFactory.parseString(
                "enabled=true, host=\" 127.0.0.1 \", port=50051")).getHost());
        assertEquals("localhost", WalletService.Config.from(ConfigFactory.parseString(
                "enabled=true, host=\"localhost \", port=50051")).getHost());
    }

    @Test
    void theMockAndADisabledWalletAcceptAnyHost() {
        assertTrue(WalletService.Config.from(ConfigFactory.parseString(
                "enabled=true, host=\"10.0.0.5\", port=50051, mock=true")).isMock());
        assertFalse(WalletService.Config.from(ConfigFactory.parseString(
                "enabled=false, host=\"10.0.0.5\", port=50051")).isEnabled());
    }

    @Test
    void mockCanBeSwitchedOn() {
        WalletService.Config config = WalletService.Config.from(ConfigFactory.parseString(
                "enabled=true, host=\"127.0.0.1\", port=50051, mock=true"));

        assertTrue(config.isMock());
    }
}
