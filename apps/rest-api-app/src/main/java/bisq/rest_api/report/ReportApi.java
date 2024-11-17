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

package bisq.rest_api.report;

import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.common.rest_api.error.RestApiException;
import bisq.common.util.CollectionUtil;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Path("/report")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Report API")
public class ReportApi {
    private final NetworkService networkService;
    private final BondedRolesService bondedRolesService;

    public ReportApi(NetworkService networkService, BondedRolesService bondedRolesService) {
        this.networkService = networkService;
        this.bondedRolesService = bondedRolesService;
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
    @Path("address-list")
    public List<String> getAddressList() {
        try {
            Set<Address> bannedAddresses = bondedRolesService.getAuthorizedBondedRolesService().getBondedRoles().stream()
                    .filter(BondedRole::isBanned)
                    .map(BondedRole::getAuthorizedBondedRole)
                    .map(AuthorizedBondedRole::getAddressByTransportTypeMap)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .flatMap(map -> map.values().stream())
                    .collect(Collectors.toSet());
            Map<TransportType, Set<Address>> seedAddressesByTransport = networkService.getSeedAddressesByTransportFromConfig();
            Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
            List<String> addresslist = seedAddressesByTransport.entrySet().stream()
                    .filter(entry -> supportedTransportTypes.contains(entry.getKey()))
                    .flatMap(entry -> entry.getValue().stream())
                    .filter(address -> !bannedAddresses.contains(address))
                    .map(Address::toString)
                    .collect(Collectors.toList());

            // Oracle Nodes
            addresslist.add("kr4yvzlhwt5binpw7js2tsfqv6mjd4klmslmcxw3c5izsaqh5vvsp6ad.onion:36185");
            addresslist.add("s2yxxqvyofzud32mxliya3dihj5rdlowagkblqqtntxhi7cbdaufqkid.onion:54467");
            return addresslist;
        } catch (Exception e) {
            throw new RestApiException(Response.Status.INTERNAL_SERVER_ERROR, "Failed to get the node address list");
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
    @Path("{address}")
    public ReportDto getReport(
            @Parameter(description = "address from which we request the report")
            @PathParam("address") String address) {
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
    @Path("reports/{addresses}")
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

        CompletableFuture<List<ReportDto>> allFutures = CompletableFutureUtils.allOf(futures);

        return allFutures.join();
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
