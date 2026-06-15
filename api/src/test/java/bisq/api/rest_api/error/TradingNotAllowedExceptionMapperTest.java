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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TradingNotAllowedExceptionMapperTest {

    @Test
    void mapsToConflictWithMessage() {
        TradingNotAllowedExceptionMapper mapper = new TradingNotAllowedExceptionMapper();

        Response response = mapper.toResponse(new TradingNotAllowedException("Trading is on halt"));

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        ErrorMessage entity = assertInstanceOf(ErrorMessage.class, response.getEntity());
        assertEquals("Trading is on halt", entity.error());
    }
}
