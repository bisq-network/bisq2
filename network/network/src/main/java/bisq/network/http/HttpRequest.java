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

import java.util.Optional;

/**
 * Descriptor for a single HTTP request issued by {@link HttpRequestService}.
 * Subclasses build one of these in {@code buildRequest(provider, requestData)}.
 *
 * <p>Use the static factories {@link #get(String)}, {@link #get(String, Pair)},
 * {@link #post(String, String, Pair, boolean)}, or
 * {@link #post(String, String, String, Pair, boolean)} rather than the canonical
 * constructor — they encode the safe defaults for each method.
 *
 * <p><b>{@code logPath}</b> — the path string the framework writes to INFO logs.
 * If your real {@code path} embeds secrets or PII (device tokens, session
 * identifiers, user-provided opaque data), pass a redacted {@code logPath} via
 * {@link #post(String, String, String, Pair, boolean)} (the structural portion
 * of the path, e.g. {@code "/v1/fcm/device/<redacted>"}). The framework never
 * inspects {@code logPath} — it is purely for log hygiene.
 */
public record HttpRequest(HttpMethod method,
                          String path,
                          String logPath,
                          Optional<String> body,
                          Optional<Pair<String, String>> header,
                          boolean retryOnServerError) {

    public static HttpRequest get(String path) {
        return new HttpRequest(HttpMethod.GET, path, path, Optional.empty(), Optional.empty(), true);
    }

    public static HttpRequest get(String path, Pair<String, String> header) {
        return new HttpRequest(HttpMethod.GET, path, path, Optional.empty(), Optional.of(header), true);
    }

    /**
     * Build a POST descriptor. The {@code path} is also used in INFO logs;
     * use {@link #post(String, String, String, Pair, boolean)} when the path
     * contains secrets or PII.
     *
     * @param retryOnServerError set to {@code true} ONLY when the server-side
     *                           operation is idempotent (duplicate execution
     *                           is harmless). See {@link HttpRequestService}
     *                           class JavaDoc for the full retry matrix.
     */
    public static HttpRequest post(String path,
                                   String body,
                                   Pair<String, String> header,
                                   boolean retryOnServerError) {
        return new HttpRequest(HttpMethod.POST, path, path, Optional.of(body), Optional.of(header), retryOnServerError);
    }

    /**
     * Build a POST descriptor with a redacted log path. Use this overload when
     * the real {@code path} contains a device token, session id, or any value
     * that should not appear in INFO logs.
     */
    public static HttpRequest post(String path,
                                   String logPath,
                                   String body,
                                   Pair<String, String> header,
                                   boolean retryOnServerError) {
        return new HttpRequest(HttpMethod.POST, path, logPath, Optional.of(body), Optional.of(header), retryOnServerError);
    }
}
