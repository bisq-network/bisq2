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

import java.util.concurrent.CompletableFuture;

public abstract class ApplicationSetup {
    protected final Config misqConfig;

    public ApplicationSetup(String config) {
        misqConfig = ConfigFactory.load(config);
        misqConfig.checkValid(ConfigFactory.defaultReference(), config);

    }

    protected Config getConfig(String path) {
        misqConfig.checkValid(ConfigFactory.defaultReference(), path);
        return misqConfig.getConfig(path);
    }

    public abstract CompletableFuture<Boolean> initialize();

    public abstract CompletableFuture<Void> shutdown();
}
