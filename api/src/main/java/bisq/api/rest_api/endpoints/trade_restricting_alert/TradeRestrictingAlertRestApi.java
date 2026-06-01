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

package bisq.api.rest_api.endpoints.trade_restricting_alert;

import bisq.api.dto.alert.AuthorizedAlertDataDto;
import bisq.api.dto.mappings.alert.AuthorizedAlertDataDtoMapping;
import bisq.api.rest_api.endpoints.RestApiBase;
import bisq.api.util.AppTypeParser;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertDataUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/trade-restricting-alert")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Trade Restricting Alert API", description = "API for retrieving the active trade restricting alert")
public class TradeRestrictingAlertRestApi extends RestApiBase {
    private final AlertService alertService;

    public TradeRestrictingAlertRestApi(AlertService alertService) {
        this.alertService = alertService;
    }

    @GET
    @Operation(
            summary = "Get the active trade restricting alert",
            description = "Returns the single highest-priority active alert that halts trading or requires a mandatory update for the requested app type. A halt-trading alert takes precedence over a require-version alert even if older.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Trade restricting alert retrieved successfully",
                            content = @Content(schema = @Schema(implementation = AuthorizedAlertDataDto.class))),
                    @ApiResponse(responseCode = "204", description = "No active trade restricting alert"),
                    @ApiResponse(responseCode = "400", description = "Bad request - invalid appType"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getTradeRestrictingAlert(@QueryParam("appType") String appTypeParam) {
        try {
            AppType appType = AppTypeParser.parse(appTypeParam);
            return AuthorizedAlertDataUtils
                    .findMostRecentTradeRestrictingAlert(alertService.getAuthorizedAlertDataSet().stream(), appType)
                    .map(AuthorizedAlertDataDtoMapping::fromBisq2Model)
                    .<Response>map(this::buildOkResponse)
                    .orElseGet(this::buildNoContentResponse);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error retrieving trade restricting alert", e);
            return buildErrorResponse("An unexpected error occurred");
        }
    }
}