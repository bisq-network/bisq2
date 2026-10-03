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

package bisq.desktop.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Browser.open passes a link to the operating system. The application opens only web URLs, because the operating
// system opens other values with other programs: on Windows a file path or a UNC path, and for other schemes the
// registered handler. See docs/specifications/desktop/links.md.
class BrowserTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "https://bisq.network/x",
            "http://localhost:8080/tx/abc",
            "http://mempool4t6mypeemozyterviq3i5de4kpoua65r3qkn5i3kknu5l2cad.onion/tx/abc",
            "HTTPS://BISQ.NETWORK/x",
            "Http://127.0.0.1:8080/tx/abc"
    })
    void acceptsHttpOrHttpsUrlWithHost(String url) {
        assertTrue(Browser.isWebUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///C:/x.exe",
            "\\\\attacker.example\\share\\x.exe",
            "//attacker.example/x",
            "javascript:alert(1)",
            "ms-msdt:/id x",
            "ms-msdt:/id%20PCWDiagnostic",
            "smb://host/share",
            "mailto:a@b.c",
            "https:///path",
            "https:bisq.network",
            "https://bisq.network/a b",
            "N/A",
            " "
    })
    void rejectsValueWhichIsNotHttpOrHttpsUrlWithHost(String value) {
        assertFalse(Browser.isWebUrl(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsNullAndEmptyValue(String value) {
        assertFalse(Browser.isWebUrl(value));
    }
}
