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

package bisq.api.access.filter.authz;

import java.net.URI;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class UriSanitizer {

    private static final int MAX_PATH_LENGTH = 2000;

    private final Pattern safePathPattern;
    private final Pattern safeQueryPattern;

    public UriSanitizer(Pattern safePathPattern, Pattern safeQueryPattern) {
        this.safePathPattern = checkNotNull(safePathPattern, "safePathPattern must not be null");
        this.safeQueryPattern = checkNotNull(safeQueryPattern, "safeQueryPattern must not be null");
    }

    public void validate(URI uri) {
        checkNotNull(uri, "uri must not be null");

        validatePath(uri.getPath());
        validateQuery(uri.getQuery());
    }

    private void validatePath(String path) {
        checkArgument(path != null, "URI path must not be null");
        checkArgument(path.startsWith("/"), "Path must start with '/'");
        checkArgument(path.length() < MAX_PATH_LENGTH, "Path exceeds max length");
        checkArgument(!path.contains(".."), "Path traversal is not allowed");
        checkArgument(safePathPattern.matcher(path).matches(), "Path contains unsafe characters");
    }

    private void validateQuery(String query) {
        if (query == null) {
            return;
        }

        checkArgument(!query.contains(".."), "Query contains path traversal");
        checkArgument(safeQueryPattern.matcher(query).matches(), "Query contains unsafe characters");
    }
}

