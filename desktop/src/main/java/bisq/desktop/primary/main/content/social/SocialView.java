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

import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.NavigationTargetTab;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import com.jfoenix.controls.JFXTabPane;
import javafx.beans.value.ChangeListener;

import javax.annotation.Nullable;

public class SocialView extends TabView<JFXTabPane, SocialModel, SocialController> {

    private final ChangeListener<Boolean> showSetupInitialUserProfileTabListener;
    @Nullable
    private NavigationTargetTab setupInitialUserProfileTab;
    private NavigationTargetTab chatTab, tradeIntentTab, userProfileTab;

    public SocialView(SocialModel model, SocialController controller) {
        super(new JFXTabPane(), model, controller);

        showSetupInitialUserProfileTabListener = (observable, oldValue, newValue) -> {
            if (!newValue) {
                root.getTabs().remove(setupInitialUserProfileTab);
                root.getTabs().setAll(chatTab, tradeIntentTab, userProfileTab);
            }
        };
    }

    @Override
    protected void createAndAddTabs() {
        chatTab = createTab(Res.get("social.chat"), NavigationTarget.CHAT);
        tradeIntentTab = createTab(Res.get("social.tradeIntent"), NavigationTarget.TRADE_INTENT);
        userProfileTab = createTab(Res.get("social.userProfile"), NavigationTarget.USER_PROFILE);
    }

    @Override
    public void onViewAttached() {
        super.onViewAttached();
        if (model.showSetupInitialUserProfileTab.get()) {
            setupInitialUserProfileTab = createTab(Res.get("social.setupInitialUserProfile"), NavigationTarget.SETUP_INITIAL_USER_PROFILE);
            root.getTabs().add(setupInitialUserProfileTab);
            model.showSetupInitialUserProfileTab.addListener(showSetupInitialUserProfileTabListener);
        } else {
            root.getTabs().setAll(chatTab, tradeIntentTab, userProfileTab);
        }
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        if (setupInitialUserProfileTab != null) {
            model.showSetupInitialUserProfileTab.removeListener(showSetupInitialUserProfileTabListener);
        }
    }
}
