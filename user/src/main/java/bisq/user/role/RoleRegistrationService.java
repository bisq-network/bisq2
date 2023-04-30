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

import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RoleRegistrationService implements PersistenceClient<RoleRegistrationServiceStore>, Service, DataService.Listener {
    private final static String REGISTRATION_PREFIX = "Registration-";
    @Getter
    private final RoleRegistrationServiceStore persistableStore = new RoleRegistrationServiceStore();
    @Getter
    private final Persistence<RoleRegistrationServiceStore> persistence;
    private final KeyPairService keyPairService;
    private final NetworkService networkService;
    @Getter
    private final ObservableSet<AuthorizedData> authorizedDataSet = new ObservableSet<>();

    public RoleRegistrationService(PersistenceService persistenceService,
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
        if (authenticatedData.getDistributedData() instanceof AuthorizedRoleRegistrationData) {
            authorizedDataSet.remove((AuthorizedData) authenticatedData);
        }
    }

    protected void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedRoleRegistrationData) {
            authorizedDataSet.add((AuthorizedData) authenticatedData);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<DataService.BroadCastDataResult> register(UserIdentity userIdentity, RoleType roleType, KeyPair keyPair) {
        String publicKeyAsHex = Hex.encode(keyPair.getPublic().getEncoded());
        AuthorizedRoleRegistrationData data = new AuthorizedRoleRegistrationData(userIdentity.getUserProfile(),
                roleType,
                publicKeyAsHex);
        if (data.getAuthorizedPublicKeys().contains(publicKeyAsHex)) {
            return networkService.publishAuthorizedData(data,
                            userIdentity.getIdentity().getNodeIdAndKeyPair(),
                            keyPair.getPrivate(),
                            keyPair.getPublic())
                    .whenComplete((result, throwable) -> {
                        if (throwable == null) {
                            getMyRegistrations().add(data);
                            persist();
                        }
                    });
        } else {
            return CompletableFuture.failedFuture(new RuntimeException("The public key is not in the list of the authorized keys yet. " +
                    "Please follow the process as described at the registration popup."));
        }
    }

    public CompletableFuture<DataService.BroadCastDataResult> removeRegistration(UserIdentity userIdentity, RoleType roleType, String publicKeyAsHex) {
        return findAuthorizedRoleRegistrationData(userIdentity.getUserProfile().getId(), roleType, publicKeyAsHex)
                .map(authorizedData -> networkService.removeAuthorizedData(authorizedData,
                                userIdentity.getIdentity().getNodeIdAndKeyPair())
                        .whenComplete((result, throwable) -> {
                            if (throwable == null) {
                                getMyRegistrations().remove((AuthorizedRoleRegistrationData) authorizedData.getDistributedData());
                                persist();
                            }
                        }))
                .orElse(CompletableFuture.completedFuture(null));
    }

    public ObservableSet<AuthorizedRoleRegistrationData> getMyRegistrations() {
        return persistableStore.getMyRegistrations();
    }

    public Optional<AuthorizedData> findAuthorizedRoleRegistrationData(String userProfileId, RoleType roleType, String publicKeyAsHex) {
        return authorizedDataSet.stream().filter(authenticatedData -> {
            AuthorizedRoleRegistrationData data = (AuthorizedRoleRegistrationData) authenticatedData.getDistributedData();
            return userProfileId.equals(data.getUserProfile().getId()) &&
                    roleType.equals(data.getRoleType()) &&
                    publicKeyAsHex.equals(data.getPublicKeyAsHex());
        }).findAny();
    }

    public boolean isRegistered(String userProfileId, RoleType roleType, String publicKeyAsHex) {
        return findAuthorizedRoleRegistrationData(userProfileId, roleType, publicKeyAsHex).isPresent();
    }

    public KeyPair findOrCreateRegistrationKey(RoleType roleType, String userProfileId) {
        String keyId = REGISTRATION_PREFIX + roleType.name() + "-" + userProfileId;
        return keyPairService.findKeyPair(keyId)
                .orElseGet(() -> keyPairService.getOrCreateKeyPair(keyId));
    }
}