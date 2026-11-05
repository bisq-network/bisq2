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

import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.DevMode;
import bisq.network.NetworkService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.support.arbitration.mu_sig.MuSigArbitratorService;
import bisq.support.mediation.mu_sig.MuSigMediatorService;
import bisq.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MuSig runs only in developer mode. The services of its mediators and arbitrators process requests which any peer can
 * send, and their checks are not complete yet, so they do not start on other nodes (see
 * docs/specifications/support/mu-sig-dispute-agents.md).
 */
@Isolated // DevMode is a static flag of the process
class MuSigDisputeAgentServicesDevModeTest {
    @Test
    void servicesDoNotProcessRequestsOutsideDeveloperMode() {
        boolean devMode = DevMode.isDevMode();
        DevMode.setDevMode(false);
        try {
            NetworkService networkService = networkService();

            arbitratorService(networkService).initialize().join();
            mediatorService(networkService).initialize().join();

            verify(networkService, never()).subscribeToConfidentialMessages(any());
            verify(networkService, never()).addConfidentialMessageListener(any());
            verify(networkService, never()).getConfidentialMessageServices();
        } finally {
            DevMode.setDevMode(devMode);
        }
    }

    @Test
    void servicesProcessRequestsInDeveloperMode() {
        boolean devMode = DevMode.isDevMode();
        DevMode.setDevMode(true);
        try {
            NetworkService networkService = networkService();

            arbitratorService(networkService).initialize().join();
            mediatorService(networkService).initialize().join();

            // The services register in one step with the replay of the processed messages
            // (docs/specifications/network/startup-replay.md)
            verify(networkService, times(2)).subscribeToConfidentialMessages(any());
            verify(networkService, never()).addConfidentialMessageListener(any());
        } finally {
            DevMode.setDevMode(devMode);
        }
    }

    private static NetworkService networkService() {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getConfidentialMessageServices()).thenReturn(Set.of());
        return networkService;
    }

    // A persistence, so that the shutdown hook of the stores finds one at the exit of the test process
    private static PersistenceService persistenceService() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        doReturn(mock(Persistence.class)).when(persistenceService).getOrCreatePersistence(any(), any(), any());
        return persistenceService;
    }

    private static MuSigArbitratorService arbitratorService(NetworkService networkService) {
        return new MuSigArbitratorService(persistenceService(),
                networkService,
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS));
    }

    private static MuSigMediatorService mediatorService(NetworkService networkService) {
        return new MuSigMediatorService(persistenceService(),
                networkService,
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS));
    }
}
