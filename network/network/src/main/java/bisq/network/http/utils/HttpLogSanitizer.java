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

import bisq.common.util.StringUtils;

/**
 * The one sanitizer for HTTP response bodies headed into a log line, shared by every client
 * so no sink drifts. A body is remote-controlled data: CR/LF can forge log lines and escape
 * sequences can corrupt a followed console, so the logged copy neutralizes control characters
 * and is capped. Only for logging — raw bodies keep flowing to callers and into
 * {@link HttpException} for classification.
 */
public class HttpLogSanitizer {
    // \p{Cntrl} alone is ASCII-only in Java: it misses the C1 range (NEL U+0085), the Unicode
    // line/paragraph separators U+2028/U+2029 that log viewers render as line breaks, and the
    // format category (Cf) whose bidi overrides like U+202E can visually reorder a log line.
    private static final String UNSAFE_FOR_LOGS = "[\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]";

    public static String loggableBody(String responseBody) {
        if (responseBody == null) {
            return "";
        }
        return StringUtils.truncate(responseBody.replaceAll(UNSAFE_FOR_LOGS, "_"), 2000);
    }
}
