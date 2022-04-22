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

package bisq.desktop.primary.main.content.social;

import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SocialView extends TabView<SocialModel, SocialController> {

    public SocialView(SocialModel model, SocialController controller) {
        super(model, controller);

        headlineLabel.setText(Res.get("welcome"));

        addTab(Res.get("social.gettingStarted"), NavigationTarget.GETTING_STARTED);
        addTab(Res.get("social.discuss"), NavigationTarget.DISCUSS);
        addTab(Res.get("social.learn"), NavigationTarget.LEARN);
        addTab(Res.get("social.connect"), NavigationTarget.CONNECT);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
