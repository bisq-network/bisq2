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

package bisq.desktop.primary.onboarding.initUserProfile;

import bisq.application.DefaultApplicationService;
import bisq.common.data.ByteArray;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.robohash.RoboHash;
import bisq.security.DigestUtil;
import bisq.security.KeyPairService;
import bisq.social.chat.ChatService;
import bisq.social.user.UserNameGenerator;
import bisq.social.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class InitUserProfileController implements Controller {
    private final InitialUserProfileModel model;
    @Getter
    private final InitialUserProfileView view;
    private final UserProfileService userProfileService;
    private final KeyPairService keyPairService;
    private final ChatService chatService;
    private Subscription nickNameSubscription;

    public InitUserProfileController(DefaultApplicationService applicationService) {
        keyPairService = applicationService.getKeyPairService();
        userProfileService = applicationService.getUserProfileService();
        chatService = applicationService.getChatService();

        model = new InitialUserProfileModel();
        view = new InitialUserProfileView(model, this);
    }

    @Override
    public void onActivate() {
        onCreateTempIdentity();
        nickNameSubscription = EasyBind.subscribe(model.nickName, e -> model.createProfileButtonDisable.set(e == null || e.isEmpty()));
    }

    @Override
    public void onDeactivate() {
        model.createProfileButtonDisable.unbind();
        nickNameSubscription.unsubscribe();
    }

    void onCreateUserProfile() {
        model.createProfileButtonDisable.set(true);
        model.showProcessingPopup.set(true);
        String profileId = model.profileId.get();
        // Notes on the first run, when a new user profile is created at startup:
        // Unfortunately the new profile can only be initialized after the network is fully initialized, which is after
        // all supported network types have a local address. This translates to longer display times for the processing
        // popup ("Preparing network node for new identity...").
        // The good news is, the user is likely to spend some time on the Create Profile popup before clicking Next,
        // which triggers the new profile initialization. The network layer is being initialized in parallel. This means
        // in practice, the processing popup won't be shown for long.
        userProfileService.createNewInitializedUserProfile(profileId,
                        model.getNickName().get(),
                        model.tempKeyId,
                        model.tempKeyPair,
                        new HashSet<>())
                .thenAccept(userProfile -> {
                    UIThread.run(() -> {
                        chatService.maybeAddDefaultChannels();
                        checkArgument(userProfile.getIdentity().domainId().equals(profileId));
                        model.showProcessingPopup.set(false);
                        UIScheduler.run(() -> Navigation.navigateTo(NavigationTarget.SELECT_USER_TYPE)).after(100);
                    });
                });
    }

    void onCreateTempIdentity() {
        model.tempKeyId = StringUtils.createUid();
        model.tempKeyPair = keyPairService.generateKeyPair();
        byte[] hash = DigestUtil.hash(model.tempKeyPair.getPublic().getEncoded());
        model.roboHashNode.set(RoboHash.getImage(new ByteArray(hash)));
        model.profileId.set(UserNameGenerator.fromHash(hash));
    }
}
