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

package bisq.api.access.filter.authn;

import bisq.common.encoding.Hex;
import bisq.common.util.StringUtils;
import bisq.security.DigestUtil;
import jakarta.ws.rs.container.ContainerRequestContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;

public class AuthUtils {
    private static final int MAX_BODY_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final int BUFFER_SIZE = 8192;

    public static String normalizePathAndQuery(URI requestUri) {
        // we strip trailing slash and add query if not empty
        String rawPath = requestUri.getRawPath();
        if (StringUtils.isEmpty(rawPath)) {
            rawPath = "/";
        } else if (rawPath.length() > 1 && rawPath.endsWith("/")) {
            rawPath = rawPath.substring(0, rawPath.length() - 1);
        }
        String rawQuery = requestUri.getRawQuery();
        String result = rawPath;
        if (StringUtils.isNotEmpty(rawQuery)) {
            result += "?" + rawQuery;
        }
        return result;
    }

    public static String getBodySha256Hex(ContainerRequestContext context) {
        // Content-Length may be spoofed; But it is an important indicator of a present body set by clients
        int declaredLength = context.getLength();

        if (declaredLength == 0) {
            return null;
        }

        // If length is known and too large, return early
        if (declaredLength > MAX_BODY_SIZE_BYTES) {
            throw new RuntimeException("Request body exceeds maximum allowed size of " + MAX_BODY_SIZE_BYTES + " bytes");
        }

        try {
            InputStream entityStream = context.getEntityStream();
            // Use declared length if larger, otherwise use default buffer size to
            // reduce unnecessary array copying if a small number was used maliciously
            int initialCapacity = Math.max(declaredLength, BUFFER_SIZE);
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(initialCapacity)) {
                byte[] chunk = new byte[BUFFER_SIZE];
                int total = 0;
                int read;
                while ((read = entityStream.read(chunk)) != -1) {
                    total += read;
                    if (total > MAX_BODY_SIZE_BYTES) {
                        throw new RuntimeException("Request body exceeds maximum allowed size of " + MAX_BODY_SIZE_BYTES + " bytes");
                    }
                    buffer.write(chunk, 0, read);
                }
                byte[] bytes = buffer.toByteArray();

                // Empty body is valid - return null to indicate no body content
                if (bytes.length == 0) {
                    return null;
                }

                context.setEntityStream(new ByteArrayInputStream(bytes));
                return Hex.encode(DigestUtil.sha256(bytes));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read request body for authentication. This will result in auth failure", e);
        }
    }
}
