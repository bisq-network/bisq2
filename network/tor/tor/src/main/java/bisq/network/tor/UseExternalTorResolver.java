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

package bisq.network.tor;

import bisq.common.application.ConfigUtil;
import bisq.common.util.StringUtils;

final class UseExternalTorResolver {
    private UseExternalTorResolver() {
    }

    static boolean evaluateUseExternalTorValue(boolean isWhonix,
                                               String torSkipLaunch,
                                               boolean useExternalTorFromJvmConfig,
                                               String useExternalTorFromConfig) {
        if (isWhonix) {
            return true;
        }

        // If environment variable is set we take that.
        if (StringUtils.isNotEmpty(torSkipLaunch)) {
            return ConfigUtil.parseBooleanFlag(torSkipLaunch);
        }

        // JVM config has higher priority than external_tor.config to make CLI/JVM overrides reliable.
        if (useExternalTorFromJvmConfig) {
            return true;
        }

        // Finally, check external_tor.config value.
        return ConfigUtil.parseBooleanFlag(useExternalTorFromConfig);
    }
}