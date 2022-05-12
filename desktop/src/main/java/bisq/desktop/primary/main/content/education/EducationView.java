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

package bisq.desktop.primary.main.content.education;

import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EducationView extends TabView<EducationModel, EducationController> {
    public EducationView(EducationModel model, EducationController controller) {
        super(model, controller);

        headlineLabel.setText(Res.get("learn"));

        addTab(Res.get("academy.overview"), NavigationTarget.ACADEMY_OVERVIEW);
        addTab(Res.get("academy.bisq"), NavigationTarget.BISQ_ACADEMY);
        addTab(Res.get("academy.bitcoin"), NavigationTarget.BITCOIN_ACADEMY);
        addTab(Res.get("academy.security"), NavigationTarget.SECURITY_ACADEMY);
        addTab(Res.get("academy.privacy"), NavigationTarget.PRIVACY_ACADEMY);
        addTab(Res.get("academy.wallets"), NavigationTarget.WALLETS_ACADEMY);
        addTab(Res.get("academy.openSource"), NavigationTarget.OPEN_SOURCE_ACADEMY);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
