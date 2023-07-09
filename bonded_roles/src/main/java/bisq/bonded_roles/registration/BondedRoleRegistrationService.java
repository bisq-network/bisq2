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

package bisq.bonded_roles.registration;

import bisq.bonded_roles.AuthorizedBondedRolesService;
import bisq.bonded_roles.AuthorizedOracleNode;
import bisq.bonded_roles.BondedRoleType;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class BondedRoleRegistrationService implements Service {
    private final NetworkService networkService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;

    public BondedRoleRegistrationService(NetworkService networkService, AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.networkService = networkService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }


    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean requestBondedRoleRegistration(String profileId,
                                                 String authorizedPublicKey,
                                                 BondedRoleType bondedRoleType,
                                                 String bondUserName,
                                                 String signatureBase64,
                                                 Map<Transport.Type, Address> addressByNetworkType,
                                                 NetworkIdWithKeyPair senderNetworkIdWithKeyPair) {
        ObservableSet<AuthorizedOracleNode> authorizedOracleNodes = authorizedBondedRolesService.getAuthorizedOracleNodes();
        if (authorizedOracleNodes.isEmpty()) {
            log.warn("authorizedOracleNodes is empty");
            return false;
        }
        BondedRoleRegistrationRequest request = new BondedRoleRegistrationRequest(profileId,
                authorizedPublicKey,
                bondedRoleType,
                bondUserName,
                signatureBase64,
                addressByNetworkType);
        authorizedOracleNodes.forEach(oracleNode ->
                networkService.confidentialSend(request, oracleNode.getNetworkId(), senderNetworkIdWithKeyPair));
        return true;
    }
}