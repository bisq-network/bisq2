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

package bisq.desktop.primary.main.content.settings.userProfile.edit;

import bisq.application.DefaultApplicationService;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.main.content.settings.userProfile.create.step2.GenerateNewProfileStep2Controller;
import bisq.desktop.primary.main.content.settings.userProfile.create.step2.GenerateNewProfileStep2Model;
import bisq.desktop.primary.main.content.settings.userProfile.create.step2.GenerateNewProfileStep2View;
import bisq.identity.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EditProfileController extends GenerateNewProfileStep2Controller {

    public EditProfileController(DefaultApplicationService applicationService) {
        super(applicationService);
    }

    @Override
    protected GenerateNewProfileStep2View createView() {
        return new EditProfileView((EditProfileModel) model, this);
    }

    @Override
    protected GenerateNewProfileStep2Model createModel() {
        return new EditProfileModel();
    }

    @Override
    public void initWithData(InitData data) {
        log.error("initWithData");
    }

    @Override
    public void onActivate() {
        UserProfile userProfile = chatUserService.getSelectedChatUserIdentity().get();
        model.getNickName().set(userProfile.getNickName());
        model.getProfileId().set(userProfile.getProfileId());
        model.getRoboHashImage().set(RoboHash.getImage(userProfile.getPubKeyHash()));
        String terms = userProfile.getPublicUserProfile().getTerms();
        if (terms == null) {
            terms = "";
        }
        model.getTerms().set(terms);
        String bio = userProfile.getPublicUserProfile().getBio();
        if (bio == null) {
            bio = "";
        }
        model.getBio().set(bio);
    }

    @Override
    protected void onSave() {
        model.getCreateProfileProgress().set(-1);
        model.getCreateProfileButtonDisabled().set(true);
        UserProfile userProfile = chatUserService.getSelectedChatUserIdentity().get();
        chatUserService.editChatUser(userProfile, model.getTerms().get(), model.getBio().get())
                .whenComplete((result, throwable) -> {
                    model.getCreateProfileProgress().set(0);
                    close();
                });
    }
}