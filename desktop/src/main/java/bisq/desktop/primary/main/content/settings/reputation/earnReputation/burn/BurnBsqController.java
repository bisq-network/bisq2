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

package bisq.desktop.primary.main.content.settings.reputation.earnReputation.burn;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.Browser;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.components.UserProfileSelection;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.social.user.ChatUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqController implements Controller {

    private final BurnBsqModel model;
    @Getter
    private final BurnBsqView view;
    private final ChatUserService chatUserService;
    private final UserProfileSelection userProfileSelection;
    private Pin selectedUserProfilePin;

    public BurnBsqController(DefaultApplicationService applicationService) {
        chatUserService = applicationService.getChatUserService();
        userProfileSelection = new UserProfileSelection(chatUserService);

        model = new BurnBsqModel();
        view = new BurnBsqView(model, this, userProfileSelection.getRoot());
    }

    @Override
    public void onActivate() {
        selectedUserProfilePin = FxBindings.subscribe(chatUserService.getSelectedChatUserIdentity(),
                chatUserIdentity -> model.getSelectedChatUserIdentity().set(chatUserIdentity)
        );
    }

    @Override
    public void onDeactivate() {
        selectedUserProfilePin.unbind();
    }

    void onClose() {
        OverlayController.hide();
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/reputation");
    }
}
