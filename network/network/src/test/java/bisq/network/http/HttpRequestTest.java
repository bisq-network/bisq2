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

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestTest {

    @Test
    void get_factoryProducesGetWithRetryOnServerErrorTrue() {
        HttpRequest req = HttpRequest.get("api/path");

        assertThat(req.method()).isEqualTo(HttpMethod.GET);
        assertThat(req.path()).isEqualTo("api/path");
        assertThat(req.logPath()).isEqualTo("api/path");
        assertThat(req.body()).isEmpty();
        assertThat(req.header()).isEmpty();
        assertThat(req.retryOnServerError()).isTrue();
    }

    @Test
    void get_withHeader_attachesHeader() {
        Pair<String, String> header = new Pair<>("X-Foo", "bar");

        HttpRequest req = HttpRequest.get("api/path", header);

        assertThat(req.method()).isEqualTo(HttpMethod.GET);
        assertThat(req.header()).contains(header);
    }

    @Test
    void post_factoryRequiresExplicitRetryFlagAndBody() {
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
    void post_canOptIntoServerErrorRetryForIdempotentEndpoints() {
        HttpRequest req = HttpRequest.post("v1/push", "body",
                new Pair<>("Content-Type", "application/json"), true);

        assertThat(req.retryOnServerError()).isTrue();
    }

    @Test
    void post_withLogPath_usesRedactedPathInLogPathField() {
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
    void serverErrorRetry_alwaysAllowedForGet() {
        HttpRequest get = HttpRequest.get("path");

        assertThat(HttpRequestService.isServerErrorRetryAllowed(get)).isTrue();
    }

    @Test
    void serverErrorRetry_postOptOutDeniesRetry() {
        HttpRequest post = HttpRequest.post("path", "body",
                new Pair<>("Content-Type", "application/json"), false);

        assertThat(HttpRequestService.isServerErrorRetryAllowed(post)).isFalse();
    }

    @Test
    void serverErrorRetry_postOptInAllowsRetry() {
        HttpRequest post = HttpRequest.post("path", "body",
                new Pair<>("Content-Type", "application/json"), true);

        assertThat(HttpRequestService.isServerErrorRetryAllowed(post)).isTrue();
    }

    @Test
    void joinUrl_collapsesDoubleSlashWhenBaseEndsWithSlashAndPathStartsWithSlash() {
        assertThat(HttpRequestService.joinUrl("https://relay.example/", "/v1/x"))
                .isEqualTo("https://relay.example/v1/x");
    }

    @Test
    void joinUrl_addsSlashWhenBaseHasNoTrailingSlashAndPathHasNoLeadingSlash() {
        assertThat(HttpRequestService.joinUrl("https://relay.example", "v1/x"))
                .isEqualTo("https://relay.example/v1/x");
    }

    @Test
    void joinUrl_normalizesMultipleTrailingSlashesOnBase() {
        assertThat(HttpRequestService.joinUrl("https://relay.example///", "/v1/x"))
                .isEqualTo("https://relay.example/v1/x");
    }
}
