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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.oracle_node.timestamp;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.reputation.data.AuthorizedTimestampData;
import bisq.user.reputation.requests.AuthorizeTimestampRequest;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimestampServiceTest {
    // A request which the node processed before the service started, for example from the mailbox, reaches the service
    // with its sender key, so the owner of the profile gets the timestamp (see
    // docs/specifications/bonded-roles/oracle-requests.md)
    @Test
    void requestWhichTheNodeProcessedBeforeTheStartIsTimestamped() {
        KeyPair sender = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizeTimestampRequest request = new AuthorizeTimestampRequest(profileId(sender), System.currentTimeMillis());
        NetworkService networkService = createNetworkService();
        processedBeforeTheStart(networkService, request, sender.getPublic());
        TimestampService service = createService(networkService);

        service.initialize().join();

        verifyTimestampPublished(networkService, request.getProfileId());
        verify(networkService).subscribeToConfidentialMessages(service);
    }

    // The profile ID is the hash of the key of the profile. Only its owner may request its timestamp (finding D9, see
    // docs/specifications/bonded-roles/oracle-requests.md).
    @Test
    void requestForTheProfileOfAnotherKeyIsNotTimestamped() {
        KeyPair owner = KeyGeneration.generateDefaultEcKeyPair();
        KeyPair otherSender = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizeTimestampRequest request = new AuthorizeTimestampRequest(profileId(owner), System.currentTimeMillis());
        NetworkService networkService = createNetworkService();
        TimestampService service = createService(networkService);

        deliverAsConfidentialMessageService(service, request, otherSender.getPublic());

        verify(networkService, never()).publishAuthorizedData(any(), any());
        assertThat(service.getPersistableStore().getTimestampsByProfileId()).isEmpty();
    }

    @Test
    void requestOfTheOwnerOfTheProfileIsTimestampedOnce() {
        KeyPair owner = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizeTimestampRequest request = new AuthorizeTimestampRequest(profileId(owner), System.currentTimeMillis());
        NetworkService networkService = createNetworkService();
        TimestampService service = createService(networkService);

        deliverAsConfidentialMessageService(service, request, owner.getPublic());

        verifyTimestampPublished(networkService, request.getProfileId());
        assertThat(service.getPersistableStore().getTimestampsByProfileId())
                .containsEntry(request.getProfileId(), request.getDate());
    }

    @Test
    void requestForAnotherProfileWhichTheNodeProcessedBeforeTheStartIsNotTimestamped() {
        KeyPair owner = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizeTimestampRequest request = new AuthorizeTimestampRequest(profileId(owner), System.currentTimeMillis());
        NetworkService networkService = createNetworkService();
        processedBeforeTheStart(networkService, request, KeyGeneration.generateDefaultEcKeyPair().getPublic());
        TimestampService service = createService(networkService);

        service.initialize().join();

        verify(networkService, never()).publishAuthorizedData(any(), any());
    }

    // As ConfidentialMessageService does for each new message
    private static void deliverAsConfidentialMessageService(TimestampService service,
                                                            AuthorizeTimestampRequest request,
                                                            PublicKey senderPublicKey) {
        service.onMessage(request);
        service.onConfidentialMessage(request, senderPublicKey);
    }

    private static void verifyTimestampPublished(NetworkService networkService, String profileId) {
        verify(networkService).publishAuthorizedData(argThat(data ->
                        data instanceof AuthorizedTimestampData timestamp && timestamp.getProfileId().equals(profileId)),
                any());
    }

    // The node processed the request before the service started. The subscription passes it to the service as a new
    // message is passed: to onMessage and to onConfidentialMessage with the sender key.
    private static void processedBeforeTheStart(NetworkService networkService,
                                                AuthorizeTimestampRequest request,
                                                PublicKey senderPublicKey) {
        doAnswer(invocation -> {
            ConfidentialMessageService.Listener listener = invocation.getArgument(0);
            listener.onMessage(request);
            listener.onConfidentialMessage(request, senderPublicKey);
            return null;
        }).when(networkService).subscribeToConfidentialMessages(any());
    }

    private static NetworkService createNetworkService() {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.publishAuthorizedData(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BroadcastResult()));
        return networkService;
    }

    private static TimestampService createService(NetworkService networkService) {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<TimestampStore> persistence = mock(Persistence.class);
        when(persistenceService.<TimestampStore>getOrCreatePersistence(any(), any(), any())).thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        KeyPair authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        return new TimestampService(persistenceService,
                networkService,
                mock(AuthorizedBondedRolesService.class),
                authorizedKeyPair.getPrivate(),
                authorizedKeyPair.getPublic(),
                false);
    }

    // The profile ID is the hash of the key of the profile
    private static String profileId(KeyPair keyPair) {
        return Hex.encode(DigestUtil.hash(keyPair.getPublic().getEncoded()));
    }
}
