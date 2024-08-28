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

package bisq.desktop.main.content.settings.network;

import bisq.common.data.Pair;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.settings.network.transport.TransportTypeController;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.common.TransportType;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.common.base.Joiner;
import javafx.scene.Node;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NetworkInfoController implements Controller {
    @Getter
    private final NetworkInfoModel model;
    @Getter
    private final NetworkInfoView view;
    private final ServiceProvider serviceProvider;
    private final UserProfileService userProfileService;
    @Getter
    private final Optional<TransportTypeController> clearNetController = Optional.empty();
    @Getter
    private final Optional<TransportTypeController> torController = Optional.empty();
    @Getter
    private final Optional<TransportTypeController> i2pController = Optional.empty();
    private final NetworkService networkService;

    public NetworkInfoController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        model = new NetworkInfoModel(networkService.getSupportedTransportTypes(),
                !networkService.isTransportTypeSupported(TransportType.CLEAR),
                !networkService.isTransportTypeSupported(TransportType.TOR),
                !networkService.isTransportTypeSupported(TransportType.I2P));

        Set<TransportType> supportedTransportTypes = serviceProvider.getNetworkService().getSupportedTransportTypes();
        view = new NetworkInfoView(model, this,
                getTransportTypeViewRoot(supportedTransportTypes, TransportType.CLEAR),
                getTransportTypeViewRoot(supportedTransportTypes, TransportType.TOR),
                getTransportTypeViewRoot(supportedTransportTypes, TransportType.I2P));
    }

    @Override
    public void onActivate() {
        AtomicInteger totalSize = new AtomicInteger();
        TreeMap<String, Set<UserProfile>> map = new TreeMap<>();
        userProfileService.getUserProfiles().forEach(userProfile -> {
            String version = userProfile.getApplicationVersion();
            if (version.isEmpty()) {
                version = Res.get("settings.network.versionDistribution.oldVersions");
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
                            return Res.get("settings.network.versionDistribution.tooltipLine", numUsers, version);
                        })
                        .toList());
        model.setVersionDistributionTooltip(info);
        log.info("Version distribution\n{}", info);
    }

    @Override
    public void onDeactivate() {
    }

    private Optional<Node> getTransportTypeViewRoot(Set<TransportType> supportedTransportTypes, TransportType type) {
        if (supportedTransportTypes.contains(type)) {
            return Optional.of(new TransportTypeController(serviceProvider, type)).map(e -> e.getView().getRoot());
        } else {
            return Optional.empty();
        }
    }
}
