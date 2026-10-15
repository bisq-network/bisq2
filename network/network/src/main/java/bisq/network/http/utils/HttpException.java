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

import lombok.Getter;

/**
 * A non-2xx HTTP response. The raw body is kept for callers that classify the response
 * ({@link #getBody()}), while the exception message carries only the sanitized, bounded form:
 * the message ends up in every log line and wrapping exception that prints the cause chain,
 * where a remote body must not inject terminal escapes or line breaks, nor be unbounded.
 */
public class HttpException extends Exception {
    @Getter
    private final int responseCode;
    @Getter
    private final String body;

    public HttpException(String body, int responseCode) {
        super(HttpLogSanitizer.loggableBody(body));
        this.body = body == null ? "" : body;
        this.responseCode = responseCode;
    }
}
