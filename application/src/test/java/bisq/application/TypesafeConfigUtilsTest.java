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

package bisq.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static bisq.application.TypesafeConfigUtils.coerce;
import static bisq.application.TypesafeConfigUtils.mapCustomArgsToTypesafeEntries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TypesafeConfigUtilsTest {

    @Test
    @DisplayName("map custom args app name with equals syntax")
    void map_custom_args_app_name_with_equals_syntax() {
        String[] args = {"--app-name=bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertFalse(result.containsKey("application.baseDir"));
    }

    @Test
    @DisplayName("map custom args app name with separate arg syntax")
    void map_custom_args_app_name_with_separate_arg_syntax() {
        String[] args = {"--app-name", "bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
    }

    @Test
    @DisplayName("map custom args data dir with equals syntax")
    void map_custom_args_data_dir_with_equals_syntax() {
        String[] args = {"--data-dir=/tmp/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("/tmp/bisq", result.get("application.baseDir"));
    }

    @Test
    @DisplayName("map custom args data dir with separate arg syntax")
    void map_custom_args_data_dir_with_separate_arg_syntax() {
        String[] args = {"--data-dir", "/tmp/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("/tmp/bisq", result.get("application.baseDir"));
    }

    @Test
    @DisplayName("map custom args multiple args together")
    void map_custom_args_multiple_args_together() {
        String[] args = {"--app-name=bisq", "--data-dir", "/opt/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertEquals("/opt/bisq", result.get("application.baseDir"));
    }

    @Test
    @DisplayName("map custom args multiple args together2")
    void map_custom_args_multiple_args_together2() {
        String[] args = {"--app-name=bisq", "--data-dir=/tmp/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertEquals("/tmp/bisq", result.get("application.baseDir"));
    }

    @Test
    @DisplayName("map custom args ignore unknown args")
    void map_custom_args_ignore_unknown_args() {
        String[] args = {"--foo=bar", "--app-name", "bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertFalse(result.containsKey("application.baseDir"));
        assertEquals(1, result.size()); // Only appName should be present
    }


    @Test
    @DisplayName("coerce boolean true")
    void coerce_boolean_true() {
        assertEquals(Boolean.TRUE, coerce("true"));
        assertEquals(Boolean.TRUE, coerce("TrUe"));
        assertEquals(Boolean.TRUE, coerce("TRUE"));
    }

    @Test
    @DisplayName("coerce boolean false")
    void coerce_boolean_false() {
        assertEquals(Boolean.FALSE, coerce("false"));
        assertEquals(Boolean.FALSE, coerce("FaLsE"));
    }

    @Test
    @DisplayName("coerce integer")
    void coerce_integer() {
        Object coerce = coerce("123");
        int expected = 123;
        assertEquals(expected, coerce);
        assertEquals(-456, coerce("-456"));
    }

    @Test
    @DisplayName("coerce long")
    void coerce_long() {
        long longValue1 = (long) Integer.MAX_VALUE + 1;
        assertEquals(longValue1, coerce(String.valueOf(longValue1)));
        long longValue2 = (long) Integer.MIN_VALUE - 1; // overflow
        assertEquals(longValue2, coerce(String.valueOf(longValue2)));
        int intValue = Integer.MIN_VALUE + 1;
        assertEquals(intValue, coerce(String.valueOf(intValue)));
    }

    @Test
    @DisplayName("coerce double")
    void coerce_double() {
        assertEquals(3.14, coerce("3.14"));
        assertEquals(-0.001, coerce("-0.001"));
        assertEquals(1.23e4, coerce("1.23e4"));
        assertEquals(-5.67E-8, coerce("-5.67E-8"));
    }

    @Test
    @DisplayName("coerce fallback string")
    void coerce_fallback_string() {
        assertEquals("hello", coerce("hello"));
        assertEquals("123abc", coerce("123abc"));
        assertEquals("1.2.3", coerce("1.2.3"));
    }

    @Test
    @DisplayName("coerce trimmed string")
    void coerce_trimmed_string() {
        assertEquals(42, coerce(" 42 "));
        assertEquals(Boolean.TRUE, coerce(" true "));
        assertEquals("  some text  ", coerce("  some text  ")); // spaces preserved if not numeric/boolean
    }
}
