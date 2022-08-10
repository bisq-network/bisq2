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

package bisq.support;

import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import bisq.user.role.AuthorizedRoleRegistrationData;
import bisq.user.role.RoleType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class MediationService implements Service, DataService.Listener, MessageListener {
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final Set<AuthorizedRoleRegistrationData> mediators = new CopyOnWriteArraySet<>();
    @Getter
    private final Observable<MediationRequest> newMediationRequest = new Observable<>();

    public MediationService(IdentityService identityService,
                            NetworkService networkService) {
        this.identityService = identityService;
        this.networkService = networkService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.addMessageListener(this);
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(service -> service.getAllAuthenticatedPayload().forEach(this::processAuthenticatedData));
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
        if (authenticatedData.getDistributedData() instanceof AuthorizedRoleRegistrationData) {
            AuthorizedRoleRegistrationData data = (AuthorizedRoleRegistrationData) authenticatedData.getDistributedData();
            if (data.getRoleType() == RoleType.MEDIATOR) {
                mediators.remove(data);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof MediationRequest) {
            processMediationRequest((MediationRequest) networkMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void requestMediation(UserIdentity myProfile, UserProfile peer) {
        MediationRequest networkMessage = new MediationRequest(myProfile.getUserProfile(), peer);
        mediators.forEach(mediator ->
                networkService.confidentialSend(networkMessage, mediator.getUserProfile().getNetworkId(), myProfile.getNodeIdAndKeyPair()).whenComplete((e, t) -> {
                    log.error("RES " + e);
                }));

    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedRoleRegistrationData) {
            AuthorizedRoleRegistrationData data = (AuthorizedRoleRegistrationData) authenticatedData.getDistributedData();
            if (data.getRoleType() == RoleType.MEDIATOR) {
                mediators.add(data);
            }
        }
    }

    private void processMediationRequest(MediationRequest mediationRequest) {
        newMediationRequest.set(mediationRequest);
    }

    public Optional<UserProfile> findMediator(String myProfileId, String userProfileId) {
        if (mediators.isEmpty()) {
            return Optional.empty();
        }
        String concat = myProfileId + userProfileId;
        int index = new BigInteger(concat.getBytes(StandardCharsets.UTF_8)).mod(BigInteger.valueOf(mediators.size())).intValue();
        log.error("index = {}", index);
        return Optional.of(new ArrayList<>(mediators).get(index).getUserProfile());
    }
}