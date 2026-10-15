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

package bisq.network.http.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpLogSanitizerTest {
    @Test
    void loggableBody_passesOrdinaryBodiesThrough() {
        assertThat(HttpLogSanitizer.loggableBody("{\"wasAccepted\":false,\"errorCode\":\"UNREGISTERED\"}"))
                .isEqualTo("{\"wasAccepted\":false,\"errorCode\":\"UNREGISTERED\"}");
    }

    @Test
    void loggableBody_neutralizesControlCharactersAndCapsLength() {
        // CR/LF forge fake log lines; ESC starts terminal escape sequences in a followed console.
        assertThat(HttpLogSanitizer.loggableBody("x\r\n2026-09-11 INFO forged line\u001B[31mred\ttab"))
                .doesNotContain("\r", "\n", "\u001B", "\t");
        assertThat(HttpLogSanitizer.loggableBody("A".repeat(5000))).hasSize(2000);
    }

    @Test
    void loggableBody_neutralizesUnicodeLineBreaksAndBidiOverrides() {
        // NEL and the Unicode line/paragraph separators render as line breaks in many log
        // viewers; the RTL override visually reorders a log line. None are ASCII controls,
        // so a plain \p{Cntrl} pattern passes them all through.
        assertThat(HttpLogSanitizer.loggableBody("a\u0085b\u2028c\u2029d\u202Ee"))
                .doesNotContain("\u0085", "\u2028", "\u2029", "\u202E")
                .isEqualTo("a_b_c_d_e");
    }

    @Test
    void loggableBody_normalizesNullToEmpty() {
        assertThat(HttpLogSanitizer.loggableBody(null)).isEmpty();
    }
}
