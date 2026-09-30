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

package bisq.api.rest_api.util;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaticFileHandlerTest {
    @Test
    void redirectsRootContextWithoutTrailingSlash() throws Exception {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        when(request.getRequestURI()).thenReturn("/doc/v1");

        new StaticFileHandler("/doc/v1/").service(request, response);

        verify(response).sendRedirect("/doc/v1/");
    }

    @Test
    void resolvesRootContextWithTrailingSlashToIndexHtml() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        when(request.getRequestURI()).thenReturn("/doc/v1/");
        when(response.getOutputStream()).thenReturn(outputStream);

        new StaticFileHandler("/doc/v1/").service(request, response);

        verify(response).setHeader("Content-Type", "text/html");
        verify(response).setStatus(200);
        assertThat(outputStream.toString(StandardCharsets.UTF_8)).contains("<title>Swagger UI</title>");
    }
}
