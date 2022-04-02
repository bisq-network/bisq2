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

package bisq.desktop.primary.main.content.social.onboarded;

import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.NavigationTargetTab;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import com.jfoenix.controls.JFXTabPane;

public class OnboardedView extends TabView<JFXTabPane, OnboardedModel, OnboardedController> {

    public OnboardedView(OnboardedModel model, OnboardedController controller) {
        super(new JFXTabPane(), model, controller);
    }

    @Override
    protected void createAndAddTabs() {
        NavigationTargetTab chatTab = createTab(Res.get("social.chat"), NavigationTarget.CHAT);
        NavigationTargetTab userProfileTab = createTab(Res.get("social.userProfile"), NavigationTarget.USER_PROFILE);
        root.getTabs().setAll(chatTab, userProfileTab);
    }

    @Override
    protected void onViewAttached() {
        //todo
        UIThread.runLater(() -> {
            NavigationTarget navigationTarget = model.getNavigationTarget();
            if (navigationTarget != null) {
                Navigation.navigateTo(navigationTarget);
            }
        });
    }

    @Override
    protected void onViewDetached() {
    }
}
