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
import bisq.social.user.profile.UserProfile;
import com.jfoenix.controls.JFXTabPane;
import javafx.beans.value.ChangeListener;

public class SocialView extends TabView<JFXTabPane, SocialModel, SocialController> {

    private NavigationTargetTab setupInitialUserProfileTab, chatTab, createOfferTab, userProfileTab;
    private ChangeListener<UserProfile> selectedUserProfileListener;

    public SocialView(SocialModel model, SocialController controller) {
        super(new JFXTabPane(), model, controller);

        selectedUserProfileListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                root.getTabs().remove(setupInitialUserProfileTab);
            }
        };
    }

    @Override
    protected void createAndAddTabs() {
        setupInitialUserProfileTab = createTab(Res.get("social.setupInitialUserProfile"), NavigationTarget.SETUP_INITIAL_USER_PROFILE);
        createOfferTab = createTab(Res.get("social.createOffer"), NavigationTarget.CREATE_SIMPLE_OFFER);
        chatTab = createTab(Res.get("social.chat"), NavigationTarget.CHAT);
        userProfileTab = createTab(Res.get("social.userProfile"), NavigationTarget.USER_PROFILE);
        root.getTabs().setAll(setupInitialUserProfileTab, createOfferTab, chatTab, userProfileTab);

        model.getSelectedUserProfile().addListener(selectedUserProfileListener);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
