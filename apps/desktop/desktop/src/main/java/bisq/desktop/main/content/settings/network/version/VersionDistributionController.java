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

package bisq.desktop.main.content.settings.network.version;

import bisq.common.data.Pair;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.settings.network.transport.TransportController;
import bisq.i18n.Res;
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
    private final ServiceProvider serviceProvider;
    @Getter
    private final Optional<TransportController> clearNetController = Optional.empty();
    @Getter
    private final Optional<TransportController> torController = Optional.empty();
    @Getter
    private final Optional<TransportController> i2pController = Optional.empty();
    private final UserProfileService userProfileService;

    public VersionDistributionController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        userProfileService = serviceProvider.getUserService().getUserProfileService();

        model = new VersionDistributionModel();
        view = new VersionDistributionView(model, this);
    }

    @Override
    public void onActivate() {
        AtomicInteger totalSize = new AtomicInteger();
        TreeMap<String, Set<UserProfile>> map = new TreeMap<>();
        userProfileService.getUserProfiles().forEach(userProfile -> {
            String version = userProfile.getApplicationVersion();
            if (version.isEmpty()) {
                version = Res.get("settings.network.version.versionDistribution.oldVersions");
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
                            return Res.get("settings.network.version.versionDistribution.tooltipLine", numUsers, version);
                        })
                        .toList());
        model.setVersionDistributionTooltip(info);
        log.info("Version distribution\n{}", info);
    }

    @Override
    public void onDeactivate() {
    }
}
