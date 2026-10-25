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

public class RelayTorrcGenerator implements TorrcConfigGenerator {
    private final TorrcConfigGenerator baseTorrcConfigGenerator;

    public RelayTorrcGenerator(TorrcConfigGenerator baseTorrcConfigGenerator) {
        this.baseTorrcConfigGenerator = baseTorrcConfigGenerator;
    }

    @Override
    public Map<String, String> generate() {
        Map<String, String> torConfigMap = baseTorrcConfigGenerator.generate();

        torConfigMap.put("ExitRelay", "1");
        torConfigMap.put("ExitPolicy", "accept 127.0.0.0/8:*,accept private:*,accept *:*,reject *:*");
        torConfigMap.put("ExitPolicyRejectPrivate", "0");

        return torConfigMap;
    }
}
