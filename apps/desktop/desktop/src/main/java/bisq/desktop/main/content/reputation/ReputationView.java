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
import bisq.desktop.main.content.ContentTabView;
import bisq.i18n.Res;

public class ReputationView extends ContentTabView<ReputationModel, ReputationController> {
    public ReputationView(ReputationModel model, ReputationController controller) {
        super(model, controller);

        addTab(Res.get("reputation.buildReputation"), NavigationTarget.BUILD_REPUTATION);
        addTab(Res.get("reputation.ranking"), NavigationTarget.REPUTATION_RANKING);
        addTab(Res.get("reputation.score"), NavigationTarget.REPUTATION_SCORE);
    }
}
