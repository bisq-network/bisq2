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

import bisq.common.network.Address;
import bisq.common.util.CollectionUtil;
import bisq.network.NetworkService;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Path("/report")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Report API")
public class ReportApi {
    private final NetworkService networkService;
    private final RestApiApplicationService applicationService;

    public ReportApi(@Context Application application) {
        applicationService = ((JaxRsApplication) application).getApplicationService().get();
        networkService = applicationService.getNetworkService();
    }

    @Operation(description = "Get a address list of seed and oracle nodes")
    @ApiResponse(responseCode = "200", description = "the list of seed and oracle node addresses",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ReportDto.class)
                    )}
    )
    @GET
    @Path("get-address-list")
    public List<String> getAddressList() {
        try {
            return applicationService.getAddressList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the node address list");
        }
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
        try {
            return fetchReportForAddress(address).join();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get report for address: " + address);
        }
    }

    @Operation(description = "Get list of reports for given comma separated addresses")
    @ApiResponse(responseCode = "200", description = "the list of reports for given comma separated addresses",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ReportDto.class)
                    )}
    )
    @GET
    @Path("get-reports/{addresses}")
    public List<ReportDto> getReports(
            @Parameter(description = "comma separated addresses from which we request the report")
            @PathParam("addresses") String addresses) {

        List<String> addressList;
        try {
            addressList = CollectionUtil.streamFromCsv(addresses).toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse addresses from CSV input: " + addresses);
        }


        List<CompletableFuture<ReportDto>> futures = addressList.stream()
                .map(this::fetchReportForAddress)
                .toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList())
                .join();
    }

    private CompletableFuture<ReportDto> fetchReportForAddress(String addressString) {
        try {
            Address address = Address.fromFullAddress(addressString);
            return networkService.requestReport(address)
                    .thenApply(report -> {
                        log.info("Report successfully created for address: {}", address);
                        return ReportDto.from(report);
                    })
                    .exceptionally(e -> {
                        log.error("Failed to get report for address: {}. Nested: {}", address, e.getMessage());
                        return ReportDto.fromError(e.getMessage());
                    });
        } catch (Exception e) {
            log.error("Error creating report for address: {}. Nested: {}", addressString, e.getMessage());
            return CompletableFuture.completedFuture(
                    ReportDto.fromError(e.getMessage())
            );
        }
    }
}
