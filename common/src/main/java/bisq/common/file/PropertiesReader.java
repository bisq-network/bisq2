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

package bisq.common.file;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class PropertiesReader {
    public static Optional<String> getOptionalConf(String[] args) {
        Optional<String> optionalPropertyFileName = Optional.empty();
        if (args.length >= 1) {
            optionalPropertyFileName = Optional.of(args[0]);
        }
        return optionalPropertyFileName;
    }

    @Nullable
    public static Properties getProperties(String propertyFileName) {
        Properties properties = new Properties();
        try {
            InputStream inputStream = PropertiesReader.class.getClassLoader().getResourceAsStream(propertyFileName);
            if (inputStream == null) {
                return null;
            }
            properties.load(inputStream);
            return properties;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Could not load property file with propertyFileName {}", propertyFileName);
            return null;
        }
    }
}
