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

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.common.application.Service;
import bisq.common.network.Address;
import bisq.api.rest_api.error.RestApiException;
import bisq.network.NetworkService;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
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

    public List<String> getAddressList() {
        try {
            return bondedRolesService.getAuthorizedBondedRolesService().getBondedRoles().stream()
                    .filter(BondedRole::isNotBanned)
                    .filter(bondedRole -> bondedRole.getAuthorizedBondedRole().getBondedRoleType() == BondedRoleType.ORACLE_NODE ||
                            bondedRole.getAuthorizedBondedRole().getBondedRoleType() == BondedRoleType.SEED_NODE)
                    .flatMap(bondedRole -> bondedRole.getAuthorizedBondedRole().getAddressByTransportTypeMap().stream()
                            .flatMap(addressMap -> addressMap.values().stream()))
                    .map(Address::getFullAddress)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RestApiException(e);
        }
    }

    public List<AddressDetailsDto> getAddressDetails(List<String> addressList) {
        return bondedRolesService.getAuthorizedBondedRolesService().getBondedRoles().stream()
                .flatMap(bondedRole -> {
                    String bondedRoleType = bondedRole.getAuthorizedBondedRole().getBondedRoleType().name();
                    String nickNameOrBondUserName = userService.getUserProfileService()
                            .findUserProfile(bondedRole.getAuthorizedBondedRole().getProfileId())
                            .map(UserProfile::getNickName)
                            .orElseGet(() -> bondedRole.getAuthorizedBondedRole().getBondUserName());

                    return bondedRole.getAuthorizedBondedRole().getAddressByTransportTypeMap().stream()
                            .flatMap(addressMap -> addressMap.values().stream()
                                    .filter(address -> addressList.contains(address.getFullAddress()))
                                    .map(address -> new AddressDetailsDto(
                                            address.getFullAddress(),
                                            bondedRoleType,
                                            nickNameOrBondUserName
                                    )));
                })
                .collect(Collectors.toList());
    }


}
