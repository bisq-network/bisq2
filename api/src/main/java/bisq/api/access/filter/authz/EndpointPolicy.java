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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public final class EndpointPolicy {
    private final Optional<Set<Pattern>> allowedEndpoints;
    private final Set<Pattern> deniedEndpoints;

    public EndpointPolicy(Optional<List<String>> allowPatterns,
                          List<String> denyPatterns) {

        checkNotNull(allowPatterns, "allowPatterns must not be null");
        checkNotNull(denyPatterns, "denyPatterns must not be null");

        this.allowedEndpoints = allowPatterns.map(list ->
                list.stream().map(Pattern::compile).collect(Collectors.toSet())
        );

        this.deniedEndpoints = denyPatterns.stream()
                .map(Pattern::compile)
                .collect(Collectors.toSet());
    }

    public void validatePathAllowed(String path) throws AuthorizationException {
        checkNotNull(path, "path must not be null");

        if (allowedEndpoints.isPresent()) {
            if (allowedEndpoints.get().isEmpty()
                    || allowedEndpoints.get().stream().noneMatch(p -> p.matcher(path).matches())) {
                throw new AuthorizationException("Path is not allowed: " + path);
            }
        }
        if (!deniedEndpoints.isEmpty()
                && deniedEndpoints.stream().anyMatch(p -> p.matcher(path).matches())) {
            throw new AuthorizationException("Path is explicitly denied: " + path);
        }
    }
}

