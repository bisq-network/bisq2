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

package bisq.desktop.primary.main.content.newProfilePopup.selectUserType;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.robohash.RoboHash;
import bisq.social.user.ChatUserIdentity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class SelectUserTypeController implements Controller {
    private final SelectUserTypeModel model;
    @Getter
    private final SelectUserTypeView view;
    private final Consumer<Boolean> navigationHandler;

    public SelectUserTypeController(DefaultApplicationService applicationService, Consumer<Boolean> navigationHandler) {
        this.navigationHandler = navigationHandler;
        ChatUserIdentity chatUserIdentity = applicationService.getChatUserService().getSelectedUserProfile().get();
        String profileId = chatUserIdentity.getProfileId();
        model = new SelectUserTypeModel(profileId, RoboHash.getImage(chatUserIdentity.getIdentity().proofOfWork().getPayload()));
        view = new SelectUserTypeView(model, this);
    }

    @Override
    public void onActivate() {
        onSelect(SelectUserTypeModel.Type.NEWBIE);
    }

    @Override
    public void onDeactivate() {
    }

    public void onSelect(SelectUserTypeModel.Type selectedType) {
        model.setSelectedType(selectedType);
    }

    public void onNext() {
        navigationHandler.accept(true);
    }

}
