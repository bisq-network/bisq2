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

package network.misq.application;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.ConfigUtil;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class ServiceProvider {
    protected final Config misqConfig;

    public ServiceProvider(String configFileName) {
        misqConfig = ConfigFactory.load(configFileName);
        misqConfig.checkValid(ConfigFactory.defaultReference(), configFileName);
    }

    protected Config getConfig(String path) {
        return ConfigUtil.getConfig(misqConfig, path);
    }

    public abstract CompletableFuture<Boolean> readAllPersisted();

    public abstract CompletableFuture<Boolean> initialize();

    public abstract CompletableFuture<Void> shutdown();
}
