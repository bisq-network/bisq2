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

import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.common.application.Service;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.common.rest_api.error.RestApiException;
import bisq.network.NetworkService;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class NodeMonitorService implements Service {
    private final NetworkService networkService;
    private final UserService userService;
    private final BondedRolesService bondedRolesService;

    public NodeMonitorService(NetworkService networkService,
                              UserService userService,
                              BondedRolesService bondedRolesService) {
        this.networkService = networkService;
        this.userService = userService;
        this.bondedRolesService = bondedRolesService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

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

    public List<AddressDetails> getAddressDetails(List<String> addressList) {
        Set<BondedRole> bondedRoles = bondedRolesService.getAuthorizedBondedRolesService().getBondedRoles();
        return bondedRoles.stream()
                .flatMap(bondedRole -> bondedRole.getAuthorizedBondedRole().getAddressByTransportTypeMap().stream().flatMap(addressMap -> addressMap.values().stream()
                        .filter(address -> addressList.contains(address.toString())) // Nutze addressList
                        .map(address -> new AddressDetails(
                                address.toString(),
                                bondedRole.getAuthorizedBondedRole().getBondedRoleType().name(),
                                userService.getUserProfileService()
                                        .findUserProfile(bondedRole.getAuthorizedBondedRole().getProfileId())
                                        .map(UserProfile::getNickName)
                                        .orElse(bondedRole.getAuthorizedBondedRole().getBondUserName())
                        ))))
                .collect(Collectors.toList());
    }


}
