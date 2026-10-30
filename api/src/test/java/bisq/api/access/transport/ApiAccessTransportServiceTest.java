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

package bisq.api.access.transport;

import bisq.api.ApiConfig;
import bisq.common.network.TransportType;
import bisq.network.NetworkService;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Node;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.TorKeyPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApiAccessTransportServiceTest {
    private static final String LAN_HOST = "192.168.1.50";
    private static final String ONION_ADDRESS = "abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuvwx.onion";

    private final NetworkService networkService = mock(NetworkService.class);
    private final KeyBundleService keyBundleService = mock(KeyBundleService.class);
    @TempDir
    Path appDataDirPath;

    @Test
    void torTransportListensOnAllAddressesWhereTheTorOfWhonixConnects() throws UnknownHostException {
        when(networkService.findOnionServiceBindAddress()).thenReturn(Optional.of(InetAddress.getByName("0.0.0.0")));

        assertThat(newService(ApiAccessTransportType.TOR, "127.0.0.1").getServerBindHost()).isEqualTo("0.0.0.0");
    }

    @Test
    void torTransportListensOnLoopbackAlsoWhenAnotherHostIsConfigured() throws UnknownHostException {
        when(networkService.findOnionServiceBindAddress()).thenReturn(Optional.of(InetAddress.getByName("127.0.0.1")));

        assertThat(newService(ApiAccessTransportType.TOR, LAN_HOST).getServerBindHost()).isEqualTo("127.0.0.1");
    }

    @Test
    void torTransportWithoutTorTransportKeepsConfiguredHost() {
        when(networkService.findOnionServiceBindAddress()).thenReturn(Optional.empty());

        assertThat(newService(ApiAccessTransportType.TOR, LAN_HOST).getServerBindHost()).isEqualTo(LAN_HOST);
    }

    @Test
    void clearTransportListensOnConfiguredHost() {
        assertThat(newService(ApiAccessTransportType.CLEAR, LAN_HOST).getServerBindHost()).isEqualTo(LAN_HOST);
        verifyNoInteractions(networkService);
    }

    // The network service completes with its first initialized transport, so with several transports Tor can come later
    @Test
    void torTransportPublishesTheOnionServiceOnceTheTorTransportHasInitialized() {
        CompletableFuture<Node> initializedTorNode = new CompletableFuture<>();
        ApiAccessTransportService service = newTorServiceWhichCanPublish(initializedTorNode);

        CompletableFuture<Boolean> initialized = service.initialize();

        assertThat(initialized).isNotDone();
        verify(networkService, never()).publishOnionService(anyInt(), anyInt(), any());

        initializedTorNode.complete(mock(Node.class));

        assertThat(initialized.join()).isTrue();
        verify(networkService).publishOnionService(eq(8090), eq(80), any());
        assertThat(service.getOnionAddress().get().getFullAddress()).isEqualTo(ONION_ADDRESS + ":80");
    }

    @Test
    void torTransportWhichFailsFailsTheApiWithoutPublishing() {
        CompletableFuture<Node> initializedTorNode = new CompletableFuture<>();
        ApiAccessTransportService service = newTorServiceWhichCanPublish(initializedTorNode);

        CompletableFuture<Boolean> initialized = service.initialize();
        initializedTorNode.completeExceptionally(new IllegalStateException("Tor did not start"));

        assertThatThrownBy(initialized::join).hasRootCauseMessage("Tor did not start");
        verify(networkService, never()).publishOnionService(anyInt(), anyInt(), any());
    }

    @Test
    void torTransportWithoutTorTransportInTheNetworkFails() {
        when(networkService.getInitializedDefaultNodeByTransport()).thenReturn(Map.of());

        assertThat(newService(ApiAccessTransportType.TOR, "127.0.0.1").awaitTransport()).isCompletedExceptionally();
    }

    @Test
    void clearTransportWaitsForNoTransport() {
        assertThat(newService(ApiAccessTransportType.CLEAR, LAN_HOST).awaitTransport()).isCompleted();
        verifyNoInteractions(networkService);
    }

    private ApiAccessTransportService newTorServiceWhichCanPublish(CompletableFuture<Node> initializedTorNode) {
        when(networkService.getInitializedDefaultNodeByTransport())
                .thenReturn(Map.of(TransportType.TOR, initializedTorNode));
        when(networkService.findServiceNode(TransportType.TOR)).thenReturn(Optional.of(mock(ServiceNode.class)));
        when(networkService.publishOnionService(anyInt(), anyInt(), any()))
                .thenReturn(CompletableFuture.completedFuture(ONION_ADDRESS));
        KeyBundle keyBundle = mock(KeyBundle.class);
        when(keyBundle.getTorKeyPair()).thenReturn(mock(TorKeyPair.class));
        when(keyBundleService.getKeyIdFromTag(anyString())).thenReturn("keyId");
        when(keyBundleService.getOrCreateKeyBundle("keyId")).thenReturn(keyBundle);
        return newService(ApiAccessTransportType.TOR, "127.0.0.1");
    }

    private ApiAccessTransportService newService(ApiAccessTransportType transportType, String bindHost) {
        ApiConfig apiConfig = new ApiConfig(transportType,
                300,
                false,
                false,
                true,
                false,
                bindHost,
                8090,
                80,
                true,
                true,
                false,
                "",
                List.of(),
                false,
                60);
        return new ApiAccessTransportService(apiConfig,
                appDataDirPath,
                networkService,
                keyBundleService,
                apiConfig.getBindPort(),
                apiConfig.getOnionServicePort());
    }
}
