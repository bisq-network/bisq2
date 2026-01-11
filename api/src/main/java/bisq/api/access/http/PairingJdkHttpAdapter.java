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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

// Used for REST only
@Slf4j
public class PairingJdkHttpAdapter implements HttpHandler {

    private final PairingRequestHandler pairingRequestHandler;

    public PairingJdkHttpAdapter(PairingRequestHandler pairingRequestHandler) {
        this.pairingRequestHandler = pairingRequestHandler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            ObjectMapper objectMapper = JsonMapperProvider.get();
            PairingRequestDto dto = objectMapper.readValue(exchange.getRequestBody(), PairingRequestDto.class);
            PairingRequest pairingRequest = PairingRequestMapper.toBisq2Model(dto);
            SessionToken sessionToken = pairingRequestHandler.handle(pairingRequest);

            PairingResponseDto pairingResponseDto = new PairingResponseDto(sessionToken.getSessionId(), sessionToken.getExpiresAt().toEpochMilli());
            byte[] responseBytes = objectMapper.writeValueAsBytes(pairingResponseDto);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid pairing request: {}", e.getMessage());
            exchange.sendResponseHeaders(400, -1);
        } catch (Exception e) {
            log.error("Pairing request failed", e);
            exchange.sendResponseHeaders(500, -1);
        }
    }
}

