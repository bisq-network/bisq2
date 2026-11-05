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

package bisq.desktop.main.content.network.p2p_network.version;

import bisq.common.application.ApplicationVersion;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.desktop.ServiceProvider;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.i18n.Res;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.transport.TorTransportService;
import bisq.network.p2p.node.transport.TransportService.TransportState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * With an external Tor Bisq uses any Tor of the user, so the local version info shows the version which the running
 * Tor reports. Only without it, it shows the Tor bundle of the build.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class VersionDistributionControllerTest {
    private static final String RUNNING_TOR_VERSION = "0.4.8.16";

    private final Observable<TransportState> torTransportState = new Observable<>(TransportState.INITIALIZE);
    private TorTransportService torTransportService;
    private VersionDistributionController controller;

    @BeforeEach
    void setUp() {
        torTransportService = mock(TorTransportService.class);
        when(torTransportService.getTransportState()).thenReturn(torTransportState);
        when(torTransportService.getTorVersion()).thenReturn(Optional.empty());
        ServiceNode torServiceNode = mock(ServiceNode.class);
        when(torServiceNode.getTransportService()).thenReturn(torTransportService);

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getUserService().getUserProfileService().getUserProfiles()).thenReturn(List.of());
        when(serviceProvider.getNetworkService().findServiceNode(TransportType.TOR))
                .thenReturn(Optional.of(torServiceNode));

        WaitForAsyncUtils.asyncFx(() -> controller = new VersionDistributionController(serviceProvider));
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void localVersionShowsTheBundleUntilTheRunningTorTellsItsVersion() {
        JavaFxToolkitExtension.runOnFxThread(() -> controller.onActivate());
        assertThat(localVersionDetails()).isEqualTo(bundleDetails());

        when(torTransportService.getTorVersion()).thenReturn(Optional.of(RUNNING_TOR_VERSION));
        torTransportState.set(TransportState.INITIALIZED);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(localVersionDetails()).isEqualTo(runningTorDetails());
    }

    @Test
    void localVersionShowsTheRunningTorWhenTheViewOpensAfterTheTransportInitialized() {
        when(torTransportService.getTorVersion()).thenReturn(Optional.of(RUNNING_TOR_VERSION));
        torTransportState.set(TransportState.INITIALIZED);

        JavaFxToolkitExtension.runOnFxThread(() -> controller.onActivate());

        assertThat(localVersionDetails()).isEqualTo(runningTorDetails());
    }

    @Test
    void deactivatedControllerIgnoresLaterTransportStates() {
        JavaFxToolkitExtension.runOnFxThread(() -> {
            controller.onActivate();
            controller.onDeactivate();
        });

        when(torTransportService.getTorVersion()).thenReturn(Optional.of(RUNNING_TOR_VERSION));
        torTransportState.set(TransportState.INITIALIZED);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(localVersionDetails()).isEqualTo(bundleDetails());
    }

    private String localVersionDetails() {
        return controller.getModel().getLocalVersionDetails().get();
    }

    private static String bundleDetails() {
        return Res.get("network.version.localVersion.details",
                ApplicationVersion.getVersion().getVersionAsString(),
                ApplicationVersion.getBuildCommitShortHash(),
                ApplicationVersion.getTorBinaryVersionString(),
                ApplicationVersion.getTorVersionString());
    }

    private static String runningTorDetails() {
        return Res.get("network.version.localVersion.details.runningTor",
                ApplicationVersion.getVersion().getVersionAsString(),
                ApplicationVersion.getBuildCommitShortHash(),
                RUNNING_TOR_VERSION);
    }
}
