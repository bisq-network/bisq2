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

package bisq.rest_api.endpoints;

import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.p2p.services.reporting.Report;
import bisq.rest_api.JaxRsApplication;
import bisq.rest_api.RestApiApplicationService;
import bisq.rest_api.dto.ReportDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Path("/report")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Report API")
public class ReportApi {
    private final NetworkService networkService;

    public ReportApi(@Context Application application) {
        RestApiApplicationService applicationService = ((JaxRsApplication) application).getApplicationService().get();
        networkService = applicationService.getNetworkService();
    }

    @Operation(description = "Get report for given address")
    @ApiResponse(responseCode = "200", description = "the report for the given address",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ReportDto.class)
                    )}
    )
    @GET
    @Path("get-report/{address}")
    public ReportDto getReport(@Parameter(description = "address from which we request the report") @PathParam("address") String address) {
        CompletableFuture<Report> future = networkService.requestReport(Address.fromFullAddress(address));
        try {
            Report report = future.get();
            log.info(report.toString());
            return ReportDto.from(report);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
