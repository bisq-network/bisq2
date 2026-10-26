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

package bisq.desktop.main.left;

import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.desktop.ServiceProvider;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.network.NetworkService;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.transport.TorTransportService;
import bisq.network.p2p.node.transport.TransportService.TransportState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Whether Bisq uses an external Tor is known once the Tor transport has initialized. The network service completes with
 * its first initialized transport, so with several transports the network info can open before that.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class NetworkInfoTest {
    private final Observable<TransportState> torTransportState = new Observable<>(TransportState.INITIALIZE);
    private TorTransportService torTransportService;
    private NetworkInfo.Controller controller;

    @BeforeEach
    void setUp() {
        torTransportService = mock(TorTransportService.class);
        when(torTransportService.getTransportState()).thenReturn(torTransportState);
        ServiceNode torServiceNode = mock(ServiceNode.class);
        when(torServiceNode.getTransportService()).thenReturn(torTransportService);
        when(torServiceNode.getPeerGroupManager()).thenReturn(Optional.empty());

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        NetworkService networkService = serviceProvider.getNetworkService();
        when(networkService.getSupportedTransportTypes()).thenReturn(Set.of(TransportType.TOR));
        when(networkService.isTransportTypeSupported(TransportType.TOR)).thenReturn(true);
        when(networkService.getServiceNodesByTransport().findServiceNode(TransportType.TOR))
                .thenReturn(Optional.of(torServiceNode));
        when(serviceProvider.getApiService().getWebSocketService()).thenReturn(Optional.empty());

        WaitForAsyncUtils.asyncFx(() -> controller = new NetworkInfo.Controller(serviceProvider, target -> {
        }));
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void externalTorIsShownOnceTheTorTransportHasInitialized() {
        JavaFxToolkitExtension.runOnFxThread(() -> controller.onActivate());
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(isExternalTorShown()).isFalse();

        when(torTransportService.isUseExternalTor()).thenReturn(true);
        torTransportState.set(TransportState.INITIALIZED);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(isExternalTorShown()).isTrue();
    }

    @Test
    void externalTorIsShownWhenTheNetworkInfoOpensAfterTheTorTransportInitialized() {
        when(torTransportService.isUseExternalTor()).thenReturn(true);
        torTransportState.set(TransportState.INITIALIZED);

        JavaFxToolkitExtension.runOnFxThread(() -> controller.onActivate());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(isExternalTorShown()).isTrue();
    }

    @Test
    void deactivatedNetworkInfoIgnoresLaterTransportStates() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            controller.onActivate();
            controller.onDeactivate();
        });
        WaitForAsyncUtils.waitForFxEvents();

        when(torTransportService.isUseExternalTor()).thenReturn(true);
        torTransportState.set(TransportState.INITIALIZED);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(isExternalTorShown()).isFalse();
    }

    private boolean isExternalTorShown() {
        return controller.getModel().getUseExternalTor().get();
    }
}
