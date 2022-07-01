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

package bisq.desktop.primary.main.content.settings.userProfile.old;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.settings.userProfile.old.components.ChannelAdmin;
import bisq.desktop.primary.main.content.settings.userProfile.old.components.CreateUserProfile;
import bisq.desktop.primary.main.content.settings.userProfile.old.components.UserProfileDisplay;
import bisq.desktop.primary.main.content.settings.userProfile.old.components.UserProfileSelectionAtSettings;
import bisq.social.chat.ChatService;
import bisq.social.user.ChatUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class UserProfileControllerOld implements Controller {
    private final UserProfileSelectionAtSettings userProfileSelection;
    @Getter
    private final UserProfileModelOld model;
    @Getter
    private final UserProfileViewOld view;
    private Subscription selectedUserProfileSubscription;

    public UserProfileControllerOld(DefaultApplicationService applicationService) {
        ChatService chatService = applicationService.getSocialService().getChatService();
        ChatUserService chatUserService = applicationService.getSocialService().getChatUserService();
        userProfileSelection = new UserProfileSelectionAtSettings(chatUserService);
        UserProfileDisplay userProfileDisplay = new UserProfileDisplay(chatUserService);
        CreateUserProfile createUserProfile = new CreateUserProfile(chatService, chatUserService, applicationService.getSecurityService());
        ChannelAdmin channelAdmin = new ChannelAdmin(chatUserService, applicationService.getSocialService().getChatService());
        model = new UserProfileModelOld(applicationService);
        view = new UserProfileViewOld(model,
                this,
                userProfileSelection.getRoot(),
                userProfileDisplay.getRoot(),
                channelAdmin.getRoot(),
                createUserProfile.getRoot());
    }

    @Override
    public void onActivate() {
        model.createUserProfileVisible.set(false);
        model.channelAdminVisible.set(true);
        selectedUserProfileSubscription = EasyBind.subscribe(userProfileSelection.getSelectedUserProfile(),
                userProfile -> {
                    /*model.channelAdminVisible.set(userProfile.getChatUser().hasEntitlementType(Role.Type.CHANNEL_ADMIN));*/
                });
    }

    @Override
    public void onDeactivate() {
        selectedUserProfileSubscription.unsubscribe();
    }

    public void showCreateUserProfile() {
        model.createUserProfileVisible.set(true);
    }
}
