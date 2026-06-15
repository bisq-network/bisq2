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
package bisq.api.rest_api.error;

import bisq.trade.exceptions.TradingNotAllowedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps {@link TradingNotAllowedException} to 409 Conflict. The trade action is rejected because a
 * security manager alert currently disallows trading (trading on halt or a min. version required),
 * which is an expected, client-actionable condition rather than a server error (the generic mapper
 * would otherwise turn it into a 500).
 */
@Slf4j
@Provider
public class TradingNotAllowedExceptionMapper implements ExceptionMapper<TradingNotAllowedException> {
    @Override
    public Response toResponse(TradingNotAllowedException exception) {
        log.info("Trade action rejected as trading is currently not allowed: {}", exception.getMessage());
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorMessage(exception.getMessage()))
                .build();
    }
}
