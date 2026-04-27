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

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface HttpClient {
    String get(String param, Optional<Pair<String, String>> optionalHeader) throws IOException;

    String post(String param, Optional<Pair<String, String>> optionalHeader) throws IOException;

    String getBaseUrl();

    /**
     * Variant of {@link #getBaseUrl()} that is safe to write to log files.
     * Identical to {@link #getBaseUrl()} unless the caller built the client
     * with a redacted form (e.g. when the path embeds device tokens, session
     * ids, or other sensitive segments).
     */
    String getLogBaseUrl();

    boolean hasPendingRequest();

    CompletableFuture<Boolean> shutdown();
}
