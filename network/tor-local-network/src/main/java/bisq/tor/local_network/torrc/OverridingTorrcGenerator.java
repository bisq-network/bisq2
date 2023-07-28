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

package bisq.tor.local_network.torrc;

import java.nio.file.Path;
import java.util.Map;

public class OverridingTorrcGenerator implements TorrcConfigGenerator {
    private final CommonTorrcGenerator template;
    private final Map<String, String> clientTorrcConfig;

    public OverridingTorrcGenerator(CommonTorrcGenerator template, Map<String, String> clientTorrcConfig) {
        this.template = template;
        this.clientTorrcConfig = clientTorrcConfig;
    }

    @Override
    public Map<String, String> generate() {
        Map<String, String> torrcConfigs = template.generate();
        torrcConfigs.putAll(clientTorrcConfig);
        return torrcConfigs;
    }

    public Path getTorrcPath() {
        return template.getThisTorNode().getTorrcPath();
    }
}
