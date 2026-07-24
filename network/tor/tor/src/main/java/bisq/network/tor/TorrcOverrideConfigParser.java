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

import bisq.common.data.Pair;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Parses the {@code torrcOverrides} HOCON object into a map of key → list of values.
 * Supported value types are numbers, strings, booleans and lists/objects of those.
 * Entries whose value type is not supported are dropped.
 */
public class TorrcOverrideConfigParser {

    static Map<String, List<String>> parse(ConfigObject torrcOverrides) {
        return torrcOverrides.entrySet().stream()
                .map(TorrcOverrideConfigParser::tryToParseTorrcOverrideEntry)
                .filter(TorrcOverrideConfigParser::isValidTorrcOverrideEntry)
                .map(TorrcOverrideConfigParser::flattenTorrcOverrideOptionalValue)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (a, b) -> b, LinkedHashMap::new));
    }

    private static Pair<String, Optional<List<String>>> tryToParseTorrcOverrideEntry(Map.Entry<String, ConfigValue> entry) {
        String key = entry.getKey();
        ConfigValue configValue = entry.getValue();

        Optional<List<String>> value = tryToParseBasicDataType(configValue)
                .map(List::of)
                .or(tryToParseList(configValue));

        return new Pair<>(key, value);
    }

    private static boolean isValidTorrcOverrideEntry(Pair<String, Optional<List<String>>> keyValuePair) {
        Optional<List<String>> optionalValue = keyValuePair.getSecond();
        return optionalValue.isPresent();
    }

    private static Pair<String, List<String>> flattenTorrcOverrideOptionalValue(Pair<String, Optional<List<String>>> keyValuePair) {
        //noinspection OptionalGetWithoutIsPresent
        return new Pair<>(keyValuePair.getFirst(), keyValuePair.getSecond().get());
    }

    private static Optional<String> tryToParseBasicDataType(ConfigValue configValue) {
        return tryToParseNumber(configValue)
                .or(tryToParseString(configValue))
                .or(tryToParseBoolean(configValue));
    }

    private static Supplier<Optional<? extends List<String>>> tryToParseList(ConfigValue configValue) {
        return () -> {
            // A HOCON array (e.g. Bridge = ["a", "b"]) has valueType() == LIST and is a ConfigList,
            // which is itself a List<ConfigValue> — it is NOT a ConfigObject, so it must not be cast to one.
            if (configValue.valueType() == ConfigValueType.LIST) {
                ConfigList configList = (ConfigList) configValue;

                List<String> values = configList.stream()
                        .map(TorrcOverrideConfigParser::tryToParseBasicDataType)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList();

                return Optional.of(values);
            }
            return Optional.empty();
        };
    }

    private static Optional<String> tryToParseNumber(ConfigValue configValue) {
        if (configValue.valueType() == ConfigValueType.NUMBER) {
            int value = (int) configValue.unwrapped();
            return Optional.of(String.valueOf(value));
        }
        return Optional.empty();
    }

    private static Supplier<Optional<? extends String>> tryToParseString(ConfigValue configValue) {
        return () -> {
            if (configValue.valueType() == ConfigValueType.STRING) {
                String value = (String) configValue.unwrapped();
                return Optional.of(value);
            }
            return Optional.empty();
        };
    }

    private static Supplier<Optional<? extends String>> tryToParseBoolean(ConfigValue configValue) {
        return () -> {
            if (configValue.valueType() == ConfigValueType.BOOLEAN) {
                boolean value = (boolean) configValue.unwrapped();
                return Optional.of(String.valueOf(value));
            }
            return Optional.empty();
        };
    }
}
