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

package bisq.common.application;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Static convenience methods for accessing typesafe configurations and values.
 */
public class ConfigUtil {

    private static final BiFunction<String, Exception, IllegalStateException> toConfigValidationException = (path, ex) ->
            new IllegalStateException(format("Error resolving configuration at path '%s'.", path), ex);


    public static Config load(String configFileName) {
        Config config = ConfigFactory.load(configFileName);
        config.checkValid(ConfigFactory.defaultReference(), configFileName);
        return config;
    }

    public static Config load(String configFileName, String path) {
        return getConfig(load(configFileName), path);
    }

    public static Config getConfig(Config config, String path) {
        config.checkValid(ConfigFactory.defaultReference(), path);
        return config.getConfig(path);
    }

    public static int getInt(Config config, String path) {
        try {
            config.checkValid(ConfigFactory.defaultReference(), path);
            return config.getInt(path);
        } catch (Exception ex) {
            throw toConfigValidationException.apply(path, ex);
        }
    }

    public static Long getLong(Config config, String path) {
        try {
            config.checkValid(ConfigFactory.defaultReference(), path);
            return config.getLong(path);
        } catch (Exception ex) {
            throw toConfigValidationException.apply(path, ex);
        }
    }

    public static List<String> getStringList(Config config, String path) {
        try {
            config.checkValid(ConfigFactory.defaultReference(), path);
            return config.getList(path).unwrapped().stream().map(Object::toString).collect(Collectors.toList());
        } catch (Exception ex) {
            throw toConfigValidationException.apply(path, ex);
        }
    }
}
