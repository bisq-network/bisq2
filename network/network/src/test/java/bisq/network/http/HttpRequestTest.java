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

package bisq.network.http;

import bisq.common.data.Pair;
import bisq.network.http.utils.HttpMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestTest {

    @Test
    @DisplayName("get factory produces get with retry on server error true")
    void get_factory_produces_get_with_retry_on_server_error_true() {
        HttpRequest req = HttpRequest.get("api/path");

        assertThat(req.method()).isEqualTo(HttpMethod.GET);
        assertThat(req.path()).isEqualTo("api/path");
        assertThat(req.logPath()).isEqualTo("api/path");
        assertThat(req.body()).isEmpty();
        assertThat(req.header()).isEmpty();
        assertThat(req.retryOnServerError()).isTrue();
    }

    @Test
    @DisplayName("get with header attaches header")
    void get_with_header_attaches_header() {
        Pair<String, String> header = new Pair<>("X-Foo", "bar");

        HttpRequest req = HttpRequest.get("api/path", header);

        assertThat(req.method()).isEqualTo(HttpMethod.GET);
        assertThat(req.header()).contains(header);
    }

    @Test
    @DisplayName("post factory requires explicit retry flag and body")
    void post_factory_requires_explicit_retry_flag_and_body() {
        Pair<String, String> header = new Pair<>("Content-Type", "application/json");

        HttpRequest req = HttpRequest.post("v1/push", "{\"a\":1}", header, false);

        assertThat(req.method()).isEqualTo(HttpMethod.POST);
        assertThat(req.path()).isEqualTo("v1/push");
        assertThat(req.logPath())
                .as("logPath defaults to path when caller does not redact")
                .isEqualTo("v1/push");
        assertThat(req.body()).contains("{\"a\":1}");
        assertThat(req.header()).contains(header);
        assertThat(req.retryOnServerError()).isFalse();
    }

    @Test
    @DisplayName("post can opt into server error retry for idempotent endpoints")
    void post_can_opt_into_server_error_retry_for_idempotent_endpoints() {
        HttpRequest req = HttpRequest.post("v1/push", "body",
                new Pair<>("Content-Type", "application/json"), true);

        assertThat(req.retryOnServerError()).isTrue();
    }

    @Test
    @DisplayName("post with log path uses redacted path in log path field")
    void post_with_log_path_uses_redacted_path_in_log_path_field() {
        HttpRequest req = HttpRequest.post("/v1/fcm/device/abc-123",
                "/v1/fcm/device/<redacted>",
                "body",
                new Pair<>("Content-Type", "application/json"),
                true);

        assertThat(req.path())
                .as("real path still carries the token for the wire")
                .isEqualTo("/v1/fcm/device/abc-123");
        assertThat(req.logPath())
                .as("logPath does not leak the token")
                .isEqualTo("/v1/fcm/device/<redacted>")
                .doesNotContain("abc-123");
    }

    @Test
    @DisplayName("server error retry always allowed for get")
    void server_error_retry_always_allowed_for_get() {
        HttpRequest get = HttpRequest.get("path");

        assertThat(HttpRequestService.isServerErrorRetryAllowed(get)).isTrue();
    }

    @Test
    @DisplayName("server error retry post opt out denies retry")
    void server_error_retry_post_opt_out_denies_retry() {
        HttpRequest post = HttpRequest.post("path", "body",
                new Pair<>("Content-Type", "application/json"), false);

        assertThat(HttpRequestService.isServerErrorRetryAllowed(post)).isFalse();
    }

    @Test
    @DisplayName("server error retry post opt in allows retry")
    void server_error_retry_post_opt_in_allows_retry() {
        HttpRequest post = HttpRequest.post("path", "body",
                new Pair<>("Content-Type", "application/json"), true);

        assertThat(HttpRequestService.isServerErrorRetryAllowed(post)).isTrue();
    }

    @Test
    @DisplayName("join url collapses double slash when base ends with slash and path starts with slash")
    void join_url_collapses_double_slash_when_base_ends_with_slash_and_path_starts_with_slash() {
        assertThat(HttpRequestService.joinUrl("https://relay.example/", "/v1/x"))
                .isEqualTo("https://relay.example/v1/x");
    }

    @Test
    @DisplayName("join url adds slash when base has no trailing slash and path has no leading slash")
    void join_url_adds_slash_when_base_has_no_trailing_slash_and_path_has_no_leading_slash() {
        assertThat(HttpRequestService.joinUrl("https://relay.example", "v1/x"))
                .isEqualTo("https://relay.example/v1/x");
    }

    @Test
    @DisplayName("join url normalizes multiple trailing slashes on base")
    void join_url_normalizes_multiple_trailing_slashes_on_base() {
        assertThat(HttpRequestService.joinUrl("https://relay.example///", "/v1/x"))
                .isEqualTo("https://relay.example/v1/x");
    }
}
