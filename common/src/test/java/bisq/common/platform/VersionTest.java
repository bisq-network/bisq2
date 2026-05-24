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

package bisq.common.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class VersionTest {

    // --- Validation ---------------------------------------------------------

    @Test
    @DisplayName("valid versions")
    void valid_versions() {
        assertTrue(Version.isValid("1"));
        assertTrue(Version.isValid("1.0"));
        assertTrue(Version.isValid("1.2.3"));
        assertTrue(Version.isValid("1.2.3.0"));
        assertTrue(Version.isValid("1.2.3.00"));
        assertTrue(Version.isValid("1.2.3.4"));
        assertTrue(Version.isValid("10.20.30.40.50"));
    }

    @Test
    @DisplayName("invalid versions")
    void invalid_versions() {
        assertFalse(Version.isValid(null));
        assertFalse(Version.isValid(""));
        assertFalse(Version.isValid("1..2"));
        assertFalse(Version.isValid("1.2."));
        assertFalse(Version.isValid(".1.2"));
        assertFalse(Version.isValid("1.2.a"));
        assertFalse(Version.isValid("v1.2.3"));
    }

    // --- Comparison ---------------------------------------------------------

    @Test
    @DisplayName("compare equal versions")
    void compare_equal_versions() {
        assertEquals(new Version("1.2.3"), new Version("1.2.3"));
        assertEquals(new Version("1.2.3"), new Version("1.2.3.0"));
        assertEquals(new Version("1.0"), new Version("1"));
    }

    @Test
    @DisplayName("compare below")
    void compare_below() {
        assertTrue(new Version("1.0").below("1.1"));
        assertTrue(new Version("1.2.3").below("1.2.4"));
        assertTrue(new Version("1.2.3").below("1.2.3.1"));
        assertTrue(new Version("1.2.3.0").below("1.2.3.1"));
        assertTrue(new Version("1.2.3.4").below("1.3"));
    }

    @Test
    @DisplayName("compare above")
    void compare_above() {
        assertTrue(new Version("1.1").above("1.0"));
        assertTrue(new Version("1.2.4").above("1.2.3"));
        assertTrue(new Version("1.2.3.1").above("1.2.3"));
        assertTrue(new Version("2.0.0").above("1.9.9.9"));
    }

    @Test
    @DisplayName("compare different lengths")
    void compare_different_lengths() {
        assertTrue(new Version("1.2.3.4").above("1.2.3"));
        assertTrue(new Version("1.2.3").below("1.2.3.4"));
        assertTrue(new Version("1.2.3").equals(new Version("1.2.3.0")));
        assertEquals(0, new Version("3.4.5").compareTo(new Version("3.4.5.0")));
        assertEquals(0, new Version("3").compareTo(new Version("3.0.0")));
    }

    // --- toString --------------------------------------------------------------

    @Test
    @DisplayName("to string")
    void to_string() {
        assertEquals("1.2.3.4", new Version("1.2.3.4").toString());
        assertEquals("10.0.7", new Version("10.0.7").toString());
    }
}
