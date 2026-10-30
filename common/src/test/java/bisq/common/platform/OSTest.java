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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The OS selects platform specific code and paths, so the os.name of each system must match exactly one OS.
 * The checks take the os.name in lower case, as {@link OS#getOsName()} returns it.
 */
class OSTest {
    @Test
    void linuxIsOnlyLinux() {
        assertThat(OS.isLinux("linux")).isTrue();
        assertThat(OS.isMacOs("linux")).isFalse();
        assertThat(OS.isWindows("linux")).isFalse();
    }

    @Test
    void macOsIsOnlyMacOs() {
        assertThat(OS.isMacOs("mac os x")).isTrue();
        assertThat(OS.isLinux("mac os x")).isFalse();
        assertThat(OS.isWindows("mac os x")).isFalse();
    }

    // "darwin" contains "win", but a JVM which reports Darwin must not take the code paths of Windows
    @Test
    void darwinIsMacOsAndNotWindows() {
        assertThat(OS.isMacOs("darwin")).isTrue();
        assertThat(OS.isWindows("darwin")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"windows 10", "windows 11", "windows server 2025"})
    void windowsIsOnlyWindows(String osName) {
        assertThat(OS.isWindows(osName)).isTrue();
        assertThat(OS.isMacOs(osName)).isFalse();
        assertThat(OS.isLinux(osName)).isFalse();
    }
}
