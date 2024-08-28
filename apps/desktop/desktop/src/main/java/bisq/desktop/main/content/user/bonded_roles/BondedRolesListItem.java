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

package bisq.desktop.main.content.user.bonded_roles;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.common.AddressByTransportTypeMap;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.common.base.Joiner;
import com.google.gson.GsonBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@ToString
public class BondedRolesListItem {
    @EqualsAndHashCode.Include
    private final BondedRole bondedRole;

    private final Optional<UserProfile> userProfile;
    private final String roleTypeString;
    private final BondedRoleType bondedRoleType;
    private final String bondUserName;
    private final String userProfileId;
    private final String signature;
    private final String userName;
    private final String address;
    private final String addressInfoJson;
    private final String isBanned;
    private final boolean staticPublicKeysProvided;
    private final boolean isRootNode;
    private final boolean isRootSeedNode;

    public BondedRolesListItem(BondedRole bondedRole, UserService userService, NetworkService networkService) {
        this.bondedRole = bondedRole;

        AuthorizedBondedRole authorizedBondedRoleData = bondedRole.getAuthorizedBondedRole();
        isBanned = bondedRole.isBanned() ? Res.get("confirmation.yes") : "";
        UserProfileService userProfileService = userService.getUserProfileService();
        userProfile = userProfileService.findUserProfile(authorizedBondedRoleData.getProfileId());
        userProfileId = userProfile.map(UserProfile::getId).orElse(Res.get("data.na"));
        userName = userProfile.map(UserProfile::getUserName).orElse(Res.get("data.na"));
        bondUserName = authorizedBondedRoleData.getBondUserName();
        signature = authorizedBondedRoleData.getSignatureBase64();
        bondedRoleType = authorizedBondedRoleData.getBondedRoleType();
        staticPublicKeysProvided = authorizedBondedRoleData.staticPublicKeysProvided();

        Optional<AddressByTransportTypeMap> addressByTransportTypeMap = authorizedBondedRoleData.getAddressByTransportTypeMap();
        if (addressByTransportTypeMap.isPresent()) {
            AddressByTransportTypeMap addressMap = addressByTransportTypeMap.get();
            List<String> list = addressMap.entrySet().stream()
                    .map(e -> e.getKey().name() + ": " + e.getValue().getFullAddress())
                    .collect(Collectors.toList());
            address = Joiner.on("\n").join(list);
            addressInfoJson = new GsonBuilder().setPrettyPrinting().create().toJson(addressMap);

            Set<String> bondedRoleAddresses = addressMap.values().stream()
                    .map(Address::getFullAddress)
                    .collect(Collectors.toSet());
            Set<String> seedAddressesFromConfig = networkService.getSeedAddressesByTransportFromConfig().values().stream()
                    .flatMap(Collection::stream)
                    .map(Address::getFullAddress)
                    .collect(Collectors.toSet());
            isRootSeedNode = bondedRoleAddresses.stream().anyMatch(seedAddressesFromConfig::contains);
        } else {
            address = "";
            addressInfoJson = "";
            isRootSeedNode = false;
        }

        isRootNode = staticPublicKeysProvided || isRootSeedNode;
        roleTypeString = isRootNode ?
                bondedRoleType.getDisplayString() + " (root)" :
                bondedRoleType.getDisplayString();
        // oracleNodePublicKeyHash = authorizedBondedRoleData.getAuthorizedOracleNode().map(AuthorizedOracleNode::getPublicKeyHash).orElseGet(() -> Res.get("data.na"));
    }
}