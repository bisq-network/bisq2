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

package bisq.desktop.main.content.academy;

import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;

public class AcademyView extends TabView<AcademyModel, AcademyController> {

    public AcademyView(AcademyModel model, AcademyController controller) {
        super(model, controller);

        // headLine.setText(Res.get("academy.learn"));

        addTab(Res.get("academy.overview"), NavigationTarget.OVERVIEW_ACADEMY);
        addTab(Res.get("academy.overview.bisq"), NavigationTarget.BISQ_ACADEMY);
        addTab(Res.get("academy.overview.bitcoin"), NavigationTarget.BITCOIN_ACADEMY);
        addTab(Res.get("academy.overview.wallets"), NavigationTarget.WALLETS_ACADEMY);
        addTab(Res.get("academy.overview.security"), NavigationTarget.SECURITY_ACADEMY);
        addTab(Res.get("academy.overview.privacy"), NavigationTarget.PRIVACY_ACADEMY);
        addTab(Res.get("academy.overview.foss"), NavigationTarget.FOSS_ACADEMY);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    @Override
    protected boolean isRightSide() {
        return false;
    }
}
