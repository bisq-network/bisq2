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

package bisq.node_monitor;

import bisq.common.network.Address;
import bisq.http_api.rest_api.error.RestApiException;
import bisq.common.util.CollectionUtil;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.reporting.Report;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Path("/report")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Report API")
public class NodeMonitorRestApi {
    private final NetworkService networkService;
    private final NodeMonitorService nodeMonitorService;

    public NodeMonitorRestApi(NetworkService networkService, NodeMonitorService nodeMonitorService) {
        this.networkService = networkService;
        this.nodeMonitorService = nodeMonitorService;
    }

    @Operation(description = "Get a address list of seed and oracle nodes")
    @ApiResponse(responseCode = "200", description = "the list of seed and oracle node addresses",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON
                    )}
    )
    @GET
    @Path("addresses")
    public List<String> getAddressList() {
        try {
            return nodeMonitorService.getAddressList();
        } catch (Exception e) {
            throw new RestApiException(e);
        }
    }


    @Operation(description = "Get report for given address")
    @ApiResponse(responseCode = "200", description = "the report for the given address",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Report.class)
                    )}
    )
    @GET
    @Path("{address}")
    public Report getReport(
            @Parameter(description = "address from which we request the report")
            @PathParam("address") String address) {
        try {
            return networkService.requestReport(Address.fromFullAddress(address)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw new RestApiException(e);
        } catch (Exception e) {
            throw new RestApiException(e);
        }
    }

    @POST
    @Path("/reports")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Get list of reports for given addresses provided in JSON format")
    @ApiResponse(responseCode = "200", description = "The list of reports for given addresses",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Report.class)
                    )}
    )
    public List<Report> getReports(@Parameter(description = "JSON array of addresses") List<String> addresses) {
        try {
            return CompletableFutureUtils.allOf(addresses.stream()
                            .map(address -> networkService.requestReport(Address.fromFullAddress(address))))
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw new RestApiException(e);
        } catch (Exception e) {
            throw new RestApiException(e);
        }
    }

    @POST
    @Path("/addresses/details")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Get address info for a set of addresses provided in JSON format")
    @ApiResponse(responseCode = "200", description = "The set of address info (host, role type, nickname, or bond name)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AddressDetails[].class)))
    public List<AddressDetails> getAddressDetails(@Parameter(description = "JSON array of addresses") List<String> addresses) {
        try {
            log.info("Received request to get address infos for: {}", addresses);
            return nodeMonitorService.getAddressDetails(addresses);
        } catch (Exception e) {
            throw new RestApiException(e);
        }
    }
}
