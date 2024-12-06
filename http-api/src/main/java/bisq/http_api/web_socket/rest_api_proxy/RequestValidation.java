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

package bisq.http_api.web_socket.rest_api_proxy;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public class RequestValidation {
    private static final Set<String> METHOD_WHITELIST = Set.of("GET", "POST");

    static String validateRequest(WebSocketRestApiRequest request) {
        String path = request.getPath();
        if (!isValidPath(path)) {
            return String.format("Invalid path: '%s'", path);
        }
        checkArgument(METHOD_WHITELIST.contains(request.getMethod()),
                "Method not supported. Supported methods are: " + METHOD_WHITELIST + ".");

        return null;
    }

    private static boolean isValidPath(String path) {
        // todo add more validation, whitelist of endpoints and
        // Only allow alphanumeric characters and some special characters like /, -, _
        // Do not allow '../' to avoid path traversal attacks
        return path != null && path.matches("^[a-zA-Z0-9/_-]+$");
    }
}
