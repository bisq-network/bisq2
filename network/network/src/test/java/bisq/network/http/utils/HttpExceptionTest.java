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

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExceptionTest {
    @Test
    void messageIsTheSanitizedBodyWhileTheBodyStaysRaw() {
        // A remote body reaches the log through every wrapper that prints the cause chain, so the
        // message must be safe to print; classifiers still need the exact bytes.
        String body = "A\u001B[31mRED\u202Eevil" + "B".repeat(50_000);

        HttpException httpException = new HttpException(body, 502);

        assertThat(httpException.getBody()).isEqualTo(body);
        assertThat(httpException.getMessage()).doesNotContain("\u001B", "\u202E").hasSizeLessThanOrEqualTo(2000);
        assertThat(new IOException("Request failed", httpException).getCause().toString())
                .doesNotContain("\u001B").hasSizeLessThan(2100);
    }

    @Test
    void nullBodyIsAnEmptyBody() {
        HttpException httpException = new HttpException(null, 500);

        assertThat(httpException.getBody()).isEmpty();
        assertThat(httpException.getMessage()).isEmpty();
    }
}
