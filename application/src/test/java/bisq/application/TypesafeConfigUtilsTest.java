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

import java.util.Map;

import static bisq.application.TypesafeConfigUtils.coerce;
import static bisq.application.TypesafeConfigUtils.mapCustomArgsToTypesafeEntries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TypesafeConfigUtilsTest {

    @Test
    void testMapCustomArgsAppNameWithEqualsSyntax() {
        String[] args = {"--app-name=bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertFalse(result.containsKey("application.baseDir"));
    }

    @Test
    void testMapCustomArgsAppNameWithSeparateArgSyntax() {
        String[] args = {"--app-name", "bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
    }

    @Test
    void testMapCustomArgsDataDirWithEqualsSyntax() {
        String[] args = {"--data-dir=/tmp/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("/tmp/bisq", result.get("application.baseDir"));
    }

    @Test
    void testMapCustomArgsDataDirWithSeparateArgSyntax() {
        String[] args = {"--data-dir", "/tmp/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("/tmp/bisq", result.get("application.baseDir"));
    }

    @Test
    void testMapCustomArgsMultipleArgsTogether() {
        String[] args = {"--app-name=bisq", "--data-dir", "/opt/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertEquals("/opt/bisq", result.get("application.baseDir"));
    }

    @Test
    void testMapCustomArgsMultipleArgsTogether2() {
        String[] args = {"--app-name=bisq", "--data-dir=/tmp/bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertEquals("/tmp/bisq", result.get("application.baseDir"));
    }

    @Test
    void testMapCustomArgsIgnoreUnknownArgs() {
        String[] args = {"--foo=bar", "--app-name", "bisq"};
        Map<String, Object> result = mapCustomArgsToTypesafeEntries(args);

        assertEquals("bisq", result.get("application.appName"));
        assertFalse(result.containsKey("application.baseDir"));
        assertEquals(1, result.size()); // Only appName should be present
    }



    @Test
    void testCoerceBooleanTrue() {
        assertEquals(Boolean.TRUE, coerce("true"));
        assertEquals(Boolean.TRUE, coerce("TrUe"));
        assertEquals(Boolean.TRUE, coerce("TRUE"));
    }

    @Test
    void testCoerceBooleanFalse() {
        assertEquals(Boolean.FALSE, coerce("false"));
        assertEquals(Boolean.FALSE, coerce("FaLsE"));
    }

    @Test
    void testCoerceInteger() {
        Object coerce = coerce("123");
        int expected = 123;
        assertEquals(expected, coerce);
        assertEquals(-456, coerce("-456"));
    }

    @Test
    void testCoerceLong() {
        long big = (long) Integer.MAX_VALUE + 1;
        assertEquals(big, coerce(String.valueOf(big)));
        int small = (int) Integer.MIN_VALUE - 1;
        assertEquals(small, coerce(String.valueOf(small)));
    }

    @Test
    void testCoerceDouble() {
        assertEquals(3.14, coerce("3.14"));
        assertEquals(-0.001, coerce("-0.001"));
        assertEquals(1.23e4, coerce("1.23e4"));
        assertEquals(-5.67E-8, coerce("-5.67E-8"));
    }

    @Test
    void testCoerceFallbackString() {
        assertEquals("hello", coerce("hello"));
        assertEquals("123abc", coerce("123abc"));
        assertEquals("1.2.3", coerce("1.2.3"));
    }

    @Test
    void testCoerceTrimmedString() {
        assertEquals(42, coerce(" 42 "));
        assertEquals(Boolean.TRUE, coerce(" true "));
        assertEquals("  some text  ", coerce("  some text  ")); // spaces preserved if not numeric/boolean
    }
}
