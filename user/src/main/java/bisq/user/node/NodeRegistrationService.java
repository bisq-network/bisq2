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

import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.user.identity.UserIdentity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class NodeRegistrationService implements PersistenceClient<NodeRegistrationServiceStore>, Service, DataService.Listener {
    private final static String REGISTRATION_PREFIX = "Registration-";
    @Getter
    private final NodeRegistrationServiceStore persistableStore = new NodeRegistrationServiceStore();
    @Getter
    private final Persistence<NodeRegistrationServiceStore> persistence;
    private final KeyPairService keyPairService;
    private final NetworkService networkService;
    @Getter
    private final ObservableSet<AuthorizedData> authorizedRoleDataSet = new ObservableSet<>();
    @Getter
    private final ObservableSet<AuthorizedData> authorizedNodeDataSet = new ObservableSet<>();

    public NodeRegistrationService(PersistenceService persistenceService,
                                   KeyPairService keyPairService,
                                   NetworkService networkService) {
        this.keyPairService = keyPairService;
        this.networkService = networkService;
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
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
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        processAuthenticatedData(authenticatedData);
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedNodeRegistrationData) {
            authorizedNodeDataSet.remove((AuthorizedData) authenticatedData);
        }
    }

    protected void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedNodeRegistrationData) {
            authorizedNodeDataSet.add((AuthorizedData) authenticatedData);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<DataService.BroadCastDataResult> registerNode(UserIdentity userIdentity,
                                                                           NodeType nodeType,
                                                                           KeyPair keyPair,
                                                                           Map<Transport.Type, Address> addressByNetworkType) {
        String publicKeyAsHex = Hex.encode(keyPair.getPublic().getEncoded());
        AuthorizedNodeRegistrationData data = new AuthorizedNodeRegistrationData(userIdentity.getUserProfile(),
                nodeType,
                publicKeyAsHex,
                addressByNetworkType);
        if (data.getAuthorizedPublicKeys().contains(publicKeyAsHex)) {
            return networkService.publishAuthorizedData(data,
                            userIdentity.getIdentity().getNodeIdAndKeyPair(),
                            keyPair.getPrivate(),
                            keyPair.getPublic())
                    .whenComplete((result, throwable) -> {
                        if (throwable == null) {
                            getMyNodeRegistrations().add(data);
                            persist();
                        }
                    });
        } else {
            return CompletableFuture.failedFuture(new RuntimeException("The public key is not in the list of the authorized keys yet. " +
                    "Please follow the process as described at the registration popup."));
        }
    }


    public CompletableFuture<DataService.BroadCastDataResult> removeNodeRegistration(UserIdentity userIdentity, NodeType nodeType, String publicKeyAsHex) {
        return findAuthorizedNodeRegistrationData(userIdentity.getUserProfile().getId(), nodeType, publicKeyAsHex)
                .map(authorizedData -> networkService.removeAuthorizedData(authorizedData,
                                userIdentity.getIdentity().getNodeIdAndKeyPair())
                        .whenComplete((result, throwable) -> {
                            if (throwable == null) {
                                getMyNodeRegistrations().remove((AuthorizedNodeRegistrationData) authorizedData.getDistributedData());
                                persist();
                            }
                        }))
                .orElse(CompletableFuture.completedFuture(null));
    }


    public ObservableSet<AuthorizedNodeRegistrationData> getMyNodeRegistrations() {
        return persistableStore.getMyNodeRegistrations();
    }


    public Optional<AuthorizedData> findAuthorizedNodeRegistrationData(String userProfileId, NodeType nodeType, String publicKeyAsHex) {
        return authorizedNodeDataSet.stream()
                .filter(authorizedData -> authorizedData.getDistributedData() instanceof AuthorizedNodeRegistrationData)
                .filter(authenticatedData -> {
                    AuthorizedNodeRegistrationData data = (AuthorizedNodeRegistrationData) authenticatedData.getDistributedData();
                    return userProfileId.equals(data.getUserProfile().getId()) &&
                            nodeType.equals(data.getNodeType()) &&
                            publicKeyAsHex.equals(data.getPublicKeyAsHex());
                }).findAny();
    }

    public boolean isNodeRegistered(String userProfileId, NodeType nodeType, String publicKeyAsHex) {
        return findAuthorizedNodeRegistrationData(userProfileId, nodeType, publicKeyAsHex).isPresent();
    }

    public KeyPair findOrCreateNodeRegistrationKey(NodeType nodeType, String userProfileId) {
        String keyId = REGISTRATION_PREFIX + nodeType.name() + "-" + userProfileId;
        return keyPairService.findKeyPair(keyId)
                .orElseGet(() -> keyPairService.getOrCreateKeyPair(keyId));
    }
}