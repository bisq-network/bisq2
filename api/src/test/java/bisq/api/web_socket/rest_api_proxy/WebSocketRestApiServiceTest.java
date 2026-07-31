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

package bisq.api.web_socket.rest_api_proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketRestApiServiceTest {
    private static final String REST_SERVER_URL = "https://127.0.0.1:8090";

    @Test
    void resolvesPathAgainstTheRestApi() throws Exception {
        assertThat(WebSocketRestApiService.resolveRestApiUri(REST_SERVER_URL, "/api/v1/settings/version"))
                .hasToString("https://127.0.0.1:8090/api/v1/settings/version");
    }

    @Test
    void keepsTheQuery() throws Exception {
        assertThat(WebSocketRestApiService.resolveRestApiUri(REST_SERVER_URL, "/api/v1/offers?market=BTC-EUR"))
                .hasToString("https://127.0.0.1:8090/api/v1/offers?market=BTC-EUR");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Turns the configured host into user info, so the request would go to the other host
            "@evil.example.com/api/v1/user",
            "@evil.example.com:443/api/v1/user",
            "//evil.example.com/api/v1/user",
            // Traversal the server would resolve only after we checked where the request goes
            "/api/v1/%2e%2e/%2e%2e/doc/v1/",
            // Not server absolute, so it would resolve against the port instead of the path
            "api/v1/user",
    })
    void rejectsPathNotAddressingTheRestServer(String path) {
        assertThatThrownBy(() -> WebSocketRestApiService.resolveRestApiUri(REST_SERVER_URL, path))
                .isInstanceOf(Exception.class);
    }

    /**
     * The literal form normalizes before the target check, so it reaches it as a plain path with the
     * host unchanged and passes it. Whether it is refused rests entirely on the URI validator, which
     * this pins so the two forms cannot drift apart.
     */
    @Test
    void rejectsLiteralTraversalOutOfTheRestApi() {
        assertThatThrownBy(() -> WebSocketRestApiService.resolveRestApiUri(REST_SERVER_URL, "/api/v1/../../doc/v1/"))
                .isInstanceOf(Exception.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsMissingPath(String path) {
        assertThatThrownBy(() -> WebSocketRestApiService.resolveRestApiUri(REST_SERVER_URL, path))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bracketsAnIpV6BindHost() {
        assertThat(WebSocketRestApiService.toUriHost("::1")).isEqualTo("[::1]");
        assertThat(WebSocketRestApiService.toUriHost("[::1]")).isEqualTo("[::1]");
        assertThat(WebSocketRestApiService.toUriHost("127.0.0.1")).isEqualTo("127.0.0.1");
    }

    @Test
    void resolvesPathAgainstAnIpV6RestApi() throws Exception {
        assertThat(WebSocketRestApiService.resolveRestApiUri("https://[::1]:8090", "/api/v1/settings/version"))
                .hasToString("https://[::1]:8090/api/v1/settings/version");
    }
}
