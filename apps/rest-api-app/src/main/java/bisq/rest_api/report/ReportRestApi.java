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
import bisq.network.p2p.services.reporting.Report;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Path("/report")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Report API")
public class ReportRestApi {
    private final NetworkService networkService;
    private final BondedRolesService bondedRolesService;
    private final UserService userService;

    public ReportRestApi(NetworkService networkService,
                         BondedRolesService bondedRolesService,
                         UserService userService) {
        this.networkService = networkService;
        this.bondedRolesService = bondedRolesService;
        this.userService = userService;
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

    @Operation(description = "Get list of reports for given comma separated addresses")
    @ApiResponse(responseCode = "200", description = "the list of reports for given comma separated addresses",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Report.class)
                    )}
    )
    @GET
    @Path("reports/{addresses}")
    public List<Report> getReports(
            @Parameter(description = "comma separated addresses from which we request the report")
            @PathParam("addresses") String addresses) {
        try {
            List<String> addressList = CollectionUtil.streamFromCsv(addresses).toList();
            return CompletableFutureUtils.allOf(addressList.stream()
                            .map(address -> networkService.requestReport(Address.fromFullAddress(address))))
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw new RestApiException(e);
        } catch (Exception e) {
            throw new RestApiException(e);
        }
    }

    @GET
    @Path("/addresses/details")
    @Operation(description = "Get address info for a set of host:port addresses")
    @ApiResponse(responseCode = "200", description = "The set of address info (host, role type, nickname or bond name)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AddressDetails[].class)))
    public List<AddressDetails> getAddressDetailsDto(
            @QueryParam("addresses") String addresses) {  // Comma-separated list
        try {
            log.info("Received request to get address infos for: {}", addresses);
            List<String> addressList = CollectionUtil.streamFromCsv(addresses).toList();
            return getAddressDetailsProtobufs(addressList);
        } catch (Exception e) {
            throw new RestApiException(e);
        }
    }

    public List<AddressDetails> getAddressDetailsProtobufs(List<String> addressList) {
        Set<BondedRole> bondedRoles = bondedRolesService.getAuthorizedBondedRolesService().getBondedRoles();
        return bondedRoles.stream()
                .flatMap(bondedRole -> bondedRole.getAuthorizedBondedRole().getAddressByTransportTypeMap()
                        .map(addressMap -> addressMap.entrySet().stream()
                                .filter(entry -> addressList.contains(entry.getValue().toString())) // Nutze addressList
                                .map(entry -> new AddressDetails(
                                        entry.getValue().toString(),
                                        bondedRole.getAuthorizedBondedRole().getBondedRoleType().name(),
                                        userService.getUserProfileService()
                                                .findUserProfile(bondedRole.getAuthorizedBondedRole().getProfileId())
                                                .map(UserProfile::getNickName)
                                                .orElse(bondedRole.getAuthorizedBondedRole().getBondUserName())
                                ))
                        ).orElse(Stream.empty()))
                .collect(Collectors.toList());
    }
}
