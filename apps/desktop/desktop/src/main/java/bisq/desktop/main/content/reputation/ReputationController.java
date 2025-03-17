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

package bisq.desktop.main.content.reputation;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.reputation.build_reputation.BuildReputationController;
import bisq.desktop.main.content.reputation.ranking.ReputationRankingController;
import bisq.desktop.main.content.reputation.score.ReputationScoreController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ReputationController extends ContentTabController<ReputationModel> {
    @Getter
    private final ReputationView view;

    public ReputationController(ServiceProvider serviceProvider) {
        super(new ReputationModel(), NavigationTarget.REPUTATION, serviceProvider);

        view = new ReputationView(model, this);
    }

    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case BUILD_REPUTATION -> Optional.of(new BuildReputationController(serviceProvider));
            case REPUTATION_RANKING -> Optional.of(new ReputationRankingController(serviceProvider));
            case REPUTATION_SCORE -> Optional.of(new ReputationScoreController(serviceProvider));
            default -> Optional.empty();
        };
    }
}
