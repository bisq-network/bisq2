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

package bisq.user.role;

import bisq.bonded_roles.node.bisq1_bridge.data.AuthorizedBondedRoleData;
import bisq.bonded_roles.node.bisq1_bridge.data.AuthorizedOracleNode;
import bisq.bonded_roles.node.bisq1_bridge.requests.AuthorizeRoleRegistrationRequest;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class RoleRegistrationService implements Service, DataService.Listener {
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    @Getter
    private final ObservableSet<AuthorizedBondedRoleData> authorizedBondedRoleDataSet = new ObservableSet<>();
    @Getter
    private final ObservableSet<AuthorizedOracleNode> authorizedOracleNodes = new ObservableSet<>();

    public RoleRegistrationService(NetworkService networkService,
                                   UserIdentityService userIdentityService) {
        this.networkService = networkService;
        this.userIdentityService = userIdentityService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAllAuthenticatedPayload()
                        .forEach(this::processAuthenticatedData));
        networkService.addDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        processAuthenticatedData(authenticatedData);
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
       /* if (authenticatedData.getDistributedData() instanceof AuthorizedRoleRegistrationData) {
            //todo
            // authorizedDataSetOfRoleRegistrationData.remove((AuthorizedData) authenticatedData);
        }*/
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean requestAuthorization(String profileId, RoleType roleType, String bondUserName, String signatureBase64) {
        try {
            if (authorizedOracleNodes.isEmpty()) {
                log.warn("authorizedOracleNodes is empty");
                return false;
            }
            return userIdentityService.findUserIdentity(profileId).map(userIdentity -> {
                        checkArgument(userIdentity.getUserProfile().getId().equals(profileId));
                        AuthorizeRoleRegistrationRequest request = new AuthorizeRoleRegistrationRequest(profileId,
                                roleType.name(),
                                bondUserName,
                                signatureBase64);
                        authorizedOracleNodes.forEach(oracleNode ->
                                networkService.confidentialSend(request, oracleNode.getNetworkId(), userIdentity.getNodeIdAndKeyPair()));
                        //todo return result
                        return true;
                    })
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error at requestAuthorization", e);
            return false;
        }
    }

    public Set<AuthorizedBondedRoleData> getMyAuthorizedBondedRoleDataSet() {
        return authorizedBondedRoleDataSet.stream()
                .filter(data -> userIdentityService.findUserIdentity(data.getProfileId()).isPresent())
                .collect(Collectors.toSet());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedOracleNode) {
            authorizedOracleNodes.add((AuthorizedOracleNode) authenticatedData.getDistributedData());
        } else if (authenticatedData.getDistributedData() instanceof AuthorizedBondedRoleData) {
            authorizedBondedRoleDataSet.add((AuthorizedBondedRoleData) authenticatedData.getDistributedData());
        }
    }
}