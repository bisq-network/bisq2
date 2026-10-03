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

package bisq.desktop.main.content.chat.message_container.list.message_box.markdown;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

// The rules are specified in docs/specifications/chat/safe-links.md
class MarkdownSecurityUtilsTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "https://bisq.network",
            "https://bisq.network/path?q=1",
            "HTTPS://bisq.network/",
            "http://localhost:8090/api",
            "https://1.2.3.4/",
            "https://[::1]:8443/",
            // An "@" behind the authority is part of the path, the query or the fragment. The reader sees the host
            // in front of it.
            "https://evil.example/@bisq.network",
            "https://evil.example/?@bisq.network",
            "https://evil.example/#@bisq.network"
    })
    void acceptsHttpUrlsWithAHostAndWithoutUserInfo(String url) {
        assertThat(MarkdownSecurityUtils.isSafeHttpUrl(url)).isTrue();
    }

    // The text in front of the "@" of the authority is user info, and the host comes behind it. The reader takes
    // the user info for the host, so the link would show a trusted name and lead to another host.
    @ParameterizedTest
    @ValueSource(strings = {
            "https://bisq.network@evil.example/",
            "http://bisq.network@evil.example/",
            "https://bisq.network:password@evil.example/",
            "https://bisq.network:443@evil.example/",
            "https://bisq.network@evil.example:8443/",
            "https://bisq.network@1.2.3.4/",
            "https://bisq.network@[::1]/",
            "https://@evil.example/",
            "https://bisq.network@evil.example"
    })
    void rejectsUrlsWithUserInfo(String url) {
        assertThat(MarkdownSecurityUtils.isSafeHttpUrl(url)).isFalse();
    }

    // An encoded "@" (%40) and a second "@" are not user info, a port consists of digits, and a host is not empty.
    // java.net.URI finds no host in such an authority, so the host rule rejects these URLs.
    @ParameterizedTest
    @ValueSource(strings = {
            "https://bisq.network%40evil.example/",
            "https://bisq.network@@evil.example/",
            "https://evil.example:abc/",
            "https://:443/",
            "https:///path",
            "https://bisq.network@"
    })
    void rejectsUrlsWithoutAHost(String url) {
        assertThat(MarkdownSecurityUtils.isSafeHttpUrl(url)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "ftp://bisq.network/",
            "file:///etc/passwd",
            "mailto:support@bisq.network",
            "bisq.network",
            "//bisq.network/"
    })
    void rejectsUrlsWithoutAnHttpScheme(String url) {
        assertThat(MarkdownSecurityUtils.isSafeHttpUrl(url)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://bisq.network/\u202Eattack",
            "https://bisq.network/\u2066attack",
            "https://bisq.network/\nattack",
            "https://bisq.network/\u0000"
    })
    void rejectsUrlsWithControlOrBidiCharacters(String url) {
        assertThat(MarkdownSecurityUtils.isSafeHttpUrl(url)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "https://bisq.network evil", "https://evil.example\\@bisq.network/"})
    void rejectsBlankAndMalformedUrls(String url) {
        assertThat(MarkdownSecurityUtils.isSafeHttpUrl(url)).isFalse();
    }

    // The renderer checks the URL again when the reader clicks the hyperlink. Both checks are one rule.
    @Test
    void openRuleIsTheHttpUrlRule() {
        assertThat(MarkdownSecurityUtils.isSafeOpenUrl("https://bisq.network/path?q=1")).isTrue();
        assertThat(MarkdownSecurityUtils.isSafeOpenUrl("https://bisq.network@evil.example/")).isFalse();
        assertThat(MarkdownSecurityUtils.isSafeOpenUrl("javascript:alert(1)")).isFalse();
    }
}
