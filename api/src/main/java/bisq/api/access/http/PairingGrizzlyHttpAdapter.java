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

package bisq.api.access.http;

import bisq.api.access.http.dto.PairingRequestDto;
import bisq.api.access.http.dto.PairingRequestMapper;
import bisq.api.access.http.dto.PairingResponseDto;
import bisq.api.access.pairing.PairingRequest;
import bisq.api.access.session.SessionToken;
import bisq.common.json.JsonMapperProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

// Used with WS server
public class PairingGrizzlyHttpAdapter extends HttpHandler {

    private final PairingRequestHandler pairingRequestHandler;

    public PairingGrizzlyHttpAdapter(PairingRequestHandler pairingRequestHandler
    ) {
        this.pairingRequestHandler = pairingRequestHandler;
    }

    @Override
    public void service(Request request, Response response) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod().getMethodString())) {
            response.setStatus(405);
            return;
        }

        try {
            ObjectMapper objectMapper = JsonMapperProvider.get();
            PairingRequestDto dto = objectMapper.readValue(request.getInputStream(), PairingRequestDto.class);
            PairingRequest pairingRequest = PairingRequestMapper.toBisq2Model(dto);
            SessionToken sessionToken = pairingRequestHandler.handle(pairingRequest);

            response.setStatus(200);
            response.setContentType("application/json");
            PairingResponseDto pairingResponseDto = new PairingResponseDto(sessionToken.getSessionId(), sessionToken.getExpiresAt().toEpochMilli());
            objectMapper.writeValue(response.getOutputStream(), pairingResponseDto);
        } catch (IllegalArgumentException e) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid pairing request\"}");
        } catch (Exception e) {
            response.setStatus(500);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Internal server error\"}");
        }
    }
}
