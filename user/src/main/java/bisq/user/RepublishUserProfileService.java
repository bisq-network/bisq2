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

package bisq.user;

import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RepublishUserProfileService implements Service, Node.Listener {
    public static final long MIN_PAUSE_TO_NEXT_REPUBLISH = TimeUnit.HOURS.toMillis(12);
    public static final long MIN_PAUSE_TO_NEXT_REFRESH = TimeUnit.MINUTES.toMillis(5);

    private final UserIdentityService userIdentityService;
    private final NetworkService networkService;
    private volatile UserIdentity selectedUserIdentity;
    private long lastPublished;
    private long lastRefreshed;
    @Nullable
    private Pin selectedUserIdentityPin;
    private int numAllConnections;
    private final Object lock = new Object();

    public RepublishUserProfileService(UserIdentityService userIdentityService, NetworkService networkService) {
        this.userIdentityService = userIdentityService;
        this.networkService = networkService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        selectedUserIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(userIdentity -> {
            if (userIdentity != null && !userIdentity.getIdentity().isDefaultTag()) {
                synchronized (lock) {
                    selectedUserIdentity = userIdentity;
                    maybeRepublishOrRefresh();
                }
            }
        });

        networkService.addDefaultNodeListener(this);
        onNumConnectionsChanged();

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeDefaultNodeListener(this);

        if (selectedUserIdentityPin != null) {
            selectedUserIdentityPin.unbind();
            selectedUserIdentityPin = null;
        }

        return CompletableFuture.completedFuture(true);
    }


    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
    }

    @Override
    public void onConnection(Connection connection) {
        onNumConnectionsChanged();
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }


    public void userActivityDetected() {
        synchronized (lock) {
            maybeRepublishOrRefresh();
        }
    }


    private void onNumConnectionsChanged() {
        synchronized (lock) {
            numAllConnections = networkService.getNumConnectionsOnAllTransports();
            if (numAllConnections >= 8) {
                publishUserProfile();
                networkService.removeDefaultNodeListener(this);
            }
        }
    }

    private void maybeRepublishOrRefresh() {
        if (selectedUserIdentity == null) {
            return;
        }
        if (numAllConnections < 4) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPublished > MIN_PAUSE_TO_NEXT_REPUBLISH) {
            publishUserProfile();
        } else if (now - lastRefreshed > MIN_PAUSE_TO_NEXT_REFRESH) {
            refreshUserProfile();
        }
    }

    private void refreshUserProfile() {
        if (selectedUserIdentity == null) {
            return;
        }
        KeyPair keyPair = selectedUserIdentity.getNetworkIdWithKeyPair().getKeyPair();
        UserProfile userProfile = selectedUserIdentity.getUserProfile();
        userIdentityService.refreshUserProfile(userProfile, keyPair);
        lastRefreshed = System.currentTimeMillis();
    }

    private void publishUserProfile() {
        if (selectedUserIdentity == null) {
            return;
        }
        KeyPair keyPair = selectedUserIdentity.getNetworkIdWithKeyPair().getKeyPair();
        UserProfile userProfile = selectedUserIdentity.getUserProfile();
        userIdentityService.publishUserProfile(userProfile, keyPair);
        lastPublished = System.currentTimeMillis();
        lastRefreshed = lastPublished;
    }
}
