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

package bisq.tor;

import com.typesafe.config.Config;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class TorServiceConfig {

    public static TorServiceConfig from(Config config) {
        Map<String, String> torrcOverrideConfigMap = new HashMap<>();
        Config torrcOverrides = config.getConfig("torrc_overrides");
        torrcOverrides.entrySet()
                .forEach(entry -> torrcOverrideConfigMap.put(
                        entry.getKey(), (String) entry.getValue().unwrapped()
                ));
        return new TorServiceConfig(torrcOverrideConfigMap);
    }

    private final Map<String, String> torrcOverrides;

    public TorServiceConfig(Map<String, String> torrcOverrides) {
        this.torrcOverrides = torrcOverrides;
    }
}
