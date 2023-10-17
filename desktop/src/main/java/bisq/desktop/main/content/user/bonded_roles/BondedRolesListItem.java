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
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import com.google.common.base.Joiner;
import com.google.gson.GsonBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@EqualsAndHashCode
@Getter
@ToString
public class BondedRolesListItem implements TableItem {
    private final Optional<UserProfile> userProfile;
    private final String roleTypeString;
    private final String bondUserName;
    private final String signature;
    private final String userProfileId;
    private final String userName;
    private final BondedRoleType bondedRoleType;
    private final String address;
    private final String addressInfoJson;
    @EqualsAndHashCode.Exclude
    private final String isBanned;
    private final boolean staticPublicKeysProvided;

    public BondedRolesListItem(BondedRole bondedRole, UserService userService) {
        AuthorizedBondedRole authorizedBondedRoleData = bondedRole.getAuthorizedBondedRole();
        isBanned = bondedRole.isBanned() ? Res.get("confirmation.yes") : "";
        userProfile = userService.getUserProfileService().findUserProfile(authorizedBondedRoleData.getProfileId());
        userProfileId = userProfile.map(UserProfile::getId).orElse(Res.get("data.na"));
        userName = userProfile.map(UserProfile::getUserName).orElse(Res.get("data.na"));
        bondUserName = authorizedBondedRoleData.getBondUserName();
        signature = authorizedBondedRoleData.getSignatureBase64();
        bondedRoleType = authorizedBondedRoleData.getBondedRoleType();
        roleTypeString = bondedRoleType.getDisplayString();

        Map<Transport.Type, Address> addressByNetworkType = authorizedBondedRoleData.getAddressByNetworkType();
        List<String> list = addressByNetworkType.entrySet().stream()
                .map(e -> e.getKey().name() + ": " + e.getValue().getFullAddress())
                .collect(Collectors.toList());
        address = Joiner.on("\n").join(list);
        addressInfoJson = new GsonBuilder().setPrettyPrinting().create().toJson(addressByNetworkType);
        staticPublicKeysProvided = authorizedBondedRoleData.staticPublicKeysProvided();
        // oracleNodePublicKeyHash = authorizedBondedRoleData.getAuthorizedOracleNode().map(AuthorizedOracleNode::getPublicKeyHash).orElseGet(() -> Res.get("data.na"));
    }
}