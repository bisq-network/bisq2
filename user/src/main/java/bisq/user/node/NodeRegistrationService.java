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

package bisq.user.node;

import bisq.bonded_roles.node.bisq1_bridge.data.AuthorizedBondedNodeData;
import bisq.bonded_roles.node.bisq1_bridge.data.AuthorizedOracleNode;
import bisq.bonded_roles.node.bisq1_bridge.requests.AuthorizeNodeRegistrationRequest;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class NodeRegistrationService implements Service, DataService.Listener {
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    @Getter
    private final ObservableSet<AuthorizedBondedNodeData> authorizedBondedNodeDataSet = new ObservableSet<>();
    @Getter
    private final ObservableSet<AuthorizedOracleNode> authorizedOracleNodes = new ObservableSet<>();

    public NodeRegistrationService(NetworkService networkService,
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
       /* if (authenticatedData.getDistributedData() instanceof AuthorizedNodeRegistrationData) {
            //todo
            //  authorizedNodeDataSet.remove((AuthorizedData) authenticatedData);
        }*/
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean requestAuthorization(String profileId,
                                        NodeType nodeType,
                                        String bondUserName,
                                        String signatureBase64,
                                        Map<Transport.Type, Address> addressByNetworkType) {
        try {
            if (authorizedOracleNodes.isEmpty()) {
                log.warn("authorizedOracleNodes is empty");
                return false;
            }
            return userIdentityService.findUserIdentity(profileId).map(userIdentity -> {
                        checkArgument(userIdentity.getUserProfile().getId().equals(profileId));
                        AuthorizeNodeRegistrationRequest request = new AuthorizeNodeRegistrationRequest(profileId,
                                nodeType.name(),
                                bondUserName,
                                signatureBase64,
                                addressByNetworkType);
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedOracleNode) {
            authorizedOracleNodes.add((AuthorizedOracleNode) authenticatedData.getDistributedData());
        } else if (authenticatedData.getDistributedData() instanceof AuthorizedBondedNodeData) {
            authorizedBondedNodeDataSet.add((AuthorizedBondedNodeData) authenticatedData.getDistributedData());
        }
    }
}