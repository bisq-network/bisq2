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

package bisq.network.tor.common.torrc;

import java.util.Map;
import java.util.Optional;

import static bisq.network.tor.common.torrc.Torrc.Keys.*;

public class Torrc {
    public static class Keys {
        public static final String CONTROL_PORT = "ControlPort";
        public static final String SOCKS_PORT = "SocksPort";
        public static final String SOCKS_5_PROXY = "Socks5Proxy";
        public static final String DISABLE_NETWORK = "DisableNetwork";
        public static final String DATA_DIRECTORY = "DataDirectory";
        public static final String HASHED_CONTROL_PASSWORD = "HashedControlPassword";
        public static final String CONTROL_PORT_WRITE_TO_FILE = "ControlPortWriteToFile";
    }

    public static class Values {
        public static class EmbeddedTor {
            // Port 'auto' means that Tor will automatically select an unused port
            public static final String CONTROL_PORT_AUTO = "127.0.0.1:auto";
            // Port '0' means to disable the SOCKS proxy functionality
            public static final String SOCKS_PORT_DISABLED = "0";
            public static final String SOCKS_PORT_AUTO = "auto";
        }

        public static class ExternalTor {
            // Tor default values
            public static final String DEFAULT_CONTROL_PORT = "127.0.0.1:9051";
            public static final String DEFAULT_SOCKS_PORT = "127.0.0.1:9050";
            public static final String DEFAULT_SOCKS_5_PROXY = "127.0.0.1:9050";
        }
    }

    // In case we have external tor configured but not provided the required torrcOverrides values,
    // we set the Tor default values. If the user has configured torrcOverrides values which are not the same as the values
    // from the Bisq embedded tor, we use those.
    // The 3 relevant torrcOverrides values are:
    // -Dapplication.network.configByTransportType.tor.torrcOverrides.Socks5Proxy=127.0.0.1:9050
    // -Dapplication.network.configByTransportType.tor.torrcOverrides.SocksPort=127.0.0.1:9050
    // -Dapplication.network.configByTransportType.tor.torrcOverrides.ControlPort=127.0.0.1:9051
    public static void maybeApplyExternalTorDefaultTorrcValues(Map<String, String> torrcConfigMap) {
        String controlPort = Optional.ofNullable(torrcConfigMap.get(CONTROL_PORT))
                .filter(value -> !value.equals(Values.EmbeddedTor.CONTROL_PORT_AUTO))
                .orElse(Values.ExternalTor.DEFAULT_CONTROL_PORT);
        torrcConfigMap.put(CONTROL_PORT, controlPort);

        String socksPort = Optional.ofNullable(torrcConfigMap.get(SOCKS_PORT))
                .filter(value -> !value.equals(Values.EmbeddedTor.SOCKS_PORT_DISABLED) &&
                        !value.equals(Values.EmbeddedTor.SOCKS_PORT_AUTO))
                .orElse(Values.ExternalTor.DEFAULT_SOCKS_PORT);
        torrcConfigMap.put(SOCKS_PORT, socksPort);

        String socks5Proxy = Optional.ofNullable(torrcConfigMap.get(SOCKS_5_PROXY))
                .orElse(Values.ExternalTor.DEFAULT_SOCKS_5_PROXY);
        torrcConfigMap.put(SOCKS_5_PROXY, socks5Proxy);
    }
}
