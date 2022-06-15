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

package bisq.desktop.primary.overlay.onboarding.profile.nickName;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.onboarding.profile.TempIdentity;
import bisq.social.user.ChatUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class AddNickNameController implements Controller {
    private final AddNickNameModel model;
    @Getter
    private final AddNickNameView view;
    private final ChatUserService chatUserService;
    private Subscription nickNameSubscription;

    public AddNickNameController(DefaultApplicationService applicationService) {
        chatUserService = applicationService.getChatUserService();
        model = new AddNickNameModel();
        view = new AddNickNameView(model, this);
    }

    public void setTempIdentity(TempIdentity tempIdentity) {
        model.setTempIdentity(tempIdentity);
    }

    @Override
    public void onActivate() {
        nickNameSubscription = EasyBind.subscribe(model.getNickName(),
                nickName -> {
                    if (nickName != null) {
                        model.getNymId().set("[" + model.getTempIdentity().profileId() + "]");
                    } else {
                        model.getNymId().set(model.getTempIdentity().profileId());
                    }
                    model.getCreateProfileButtonDisabled().set(model.getCreateProfileProgress().get() == -1 &&
                            (nickName == null || nickName.isEmpty()));
                });
    }

    @Override
    public void onDeactivate() {
        nickNameSubscription.unsubscribe();
    }

    void onCreateUserProfile() {
        model.getCreateProfileProgress().set(-1);
        TempIdentity tempIdentity = model.getTempIdentity();
        chatUserService.createNewInitializedUserProfile(tempIdentity.profileId(),
                        model.getNickName().get(),
                        tempIdentity.tempKeyId(),
                        tempIdentity.tempKeyPair(),
                        tempIdentity.proofOfWork(),
                        "",
                        "")
                .thenCompose(chatUserService::publishNewChatUser)
                .thenAccept(chatUserIdentity -> UIThread.run(() -> {
                    checkArgument(chatUserIdentity.getIdentity().domainId().equals(tempIdentity.profileId()));
                    Navigation.navigateTo(NavigationTarget.ONBOARDING_BISQ_EASY);
                    model.getCreateProfileProgress().set(0);
                }));
    }
}
