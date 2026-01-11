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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class HttpEndpointValidator {
    private static final Pattern DEFAULT_SAFE_PATH = Pattern.compile("^[a-zA-Z0-9/_\\-,.:=~]*$");
    private static final Pattern DEFAULT_SAFE_QUERY = Pattern.compile("^[a-zA-Z0-9/_\\-,.:?&=~]*$");
    private static final Set<String> METHOD_WHITELIST = Set.of("GET", "POST", "DELETE", "PUT", "PATCH");

    private final EndpointPolicy endpointPolicy;
    private final UriSanitizer uriSanitizer;

    public HttpEndpointValidator(Optional<List<String>> allowEndpoints,
                                 List<String> denyEndpoints) {
        this(new EndpointPolicy(allowEndpoints, denyEndpoints),
                new UriSanitizer(DEFAULT_SAFE_PATH, DEFAULT_SAFE_QUERY));
    }

    public HttpEndpointValidator(EndpointPolicy endpointPolicy,
                                 UriSanitizer uriSanitizer) {
        this.endpointPolicy = endpointPolicy;
        this.uriSanitizer = uriSanitizer;
    }

    /**
     * Validates a raw URI string.
     * Throws {@link AuthorizationException} on failure.
     */
    public void validate(String rawUri) throws AuthorizationException {
        try {
            validate(URI.create(rawUri));
        } catch (AuthorizationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthorizationException("Invalid URI syntax: " + rawUri, e);
        }
    }

    /**
     * Validates a parsed URI.
     * Throws {@link AuthorizationException} on failure.
     */
    public void validate(URI uri) throws AuthorizationException {
        try {
            uriSanitizer.validate(uri);
            endpointPolicy.validatePathAllowed(uri.getPath());
        } catch (Exception e) {
            throw new AuthorizationException("Invalid URI syntax: " + uri.getRawPath(), e);
        }
    }
}
