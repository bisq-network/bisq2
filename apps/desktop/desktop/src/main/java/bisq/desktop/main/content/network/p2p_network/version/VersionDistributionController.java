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
import bisq.common.data.Pair;
import bisq.common.network.TransportType;
import bisq.common.observable.Pin;
import bisq.common.platform.Version;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.network.p2p_network.transport.TransportController;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.node.transport.TorTransportService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class VersionDistributionController implements Controller {
    @Getter
    private final VersionDistributionModel model;
    @Getter
    private final VersionDistributionView view;
    @Getter
    private final Optional<TransportController> clearNetController = Optional.empty();
    @Getter
    private final Optional<TransportController> torController = Optional.empty();
    @Getter
    private final Optional<TransportController> i2pController = Optional.empty();
    private final UserProfileService userProfileService;
    private final NetworkService networkService;
    private Optional<Pin> torTransportStatePin = Optional.empty();

    public VersionDistributionController(ServiceProvider serviceProvider) {
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        networkService = serviceProvider.getNetworkService();

        model = new VersionDistributionModel();
        view = new VersionDistributionView(model, this);
    }

    @Override
    public void onActivate() {
        AtomicInteger totalSize = new AtomicInteger();
        TreeMap<String, Set<UserProfile>> map = new TreeMap<>();
        userProfileService.getUserProfiles().forEach(userProfile -> {
            String version = userProfile.getApplicationVersion();
            if (version.isEmpty() || new Version(version).belowOrEqual(new Version("2.0.4"))) {
                version = Res.get("network.version.versionDistribution.oldVersions");
            }
            map.putIfAbsent(version, new HashSet<>());
            if (!map.get(version).contains(userProfile)) {
                totalSize.incrementAndGet();
                map.get(version).add(userProfile);
            }
        });

        model.getVersionDistribution().clear();
        model.getVersionDistribution().addAll(map.entrySet().stream()
                .map(e -> new Pair<>(e.getKey(), e.getValue().size() / (double) totalSize.get()))
                .toList());

        String info = Joiner.on("\n")
                .join(map.entrySet().stream()
                        .map(entry -> {
                            String version = entry.getKey();
                            int numUsers = entry.getValue().size();
                            return Res.get("network.version.versionDistribution.tooltipLine", numUsers, version);
                        })
                        .toList());
        model.setVersionDistributionTooltip(info);
        log.info("Version distribution\n{}", info);

        model.getLocalVersionDetails().set(getLocalVersionDetails(Optional.empty()));
        // The running Tor tells its version once the Tor transport has initialized, which can be after this view opens
        torTransportStatePin = networkService.findServiceNode(TransportType.TOR)
                .map(serviceNode -> (TorTransportService) serviceNode.getTransportService())
                .map(torTransportService -> torTransportService.getTransportState().addObserver(transportState ->
                        UIThread.run(() -> model.getLocalVersionDetails()
                                .set(getLocalVersionDetails(torTransportService.getTorVersion())))));
    }

    @Override
    public void onDeactivate() {
        torTransportStatePin.ifPresent(Pin::unbind);
        torTransportStatePin = Optional.empty();
    }

    private static String getLocalVersionDetails(Optional<String> runningTorVersion) {
        String versionString = ApplicationVersion.getVersion().getVersionAsString();
        String commitHash = ApplicationVersion.getBuildCommitShortHash();
        if (runningTorVersion.isPresent()) {
            return Res.get("network.version.localVersion.details.runningTor",
                    versionString, commitHash, runningTorVersion.get());
        }

        // Without the version of the running Tor, the version of the Tor bundle of this build
        return Res.get("network.version.localVersion.details",
                versionString, commitHash,
                ApplicationVersion.getTorBinaryVersionString(),
                ApplicationVersion.getTorVersionString());
    }
}
