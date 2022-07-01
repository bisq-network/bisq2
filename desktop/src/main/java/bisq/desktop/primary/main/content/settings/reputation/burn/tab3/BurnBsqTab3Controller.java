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

package bisq.desktop.primary.main.content.settings.reputation.burn.tab3;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.Browser;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.components.UserProfileSelection;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.social.user.ChatUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqTab3Controller implements Controller {

    private final BurnBsqTab3Model model;
    @Getter
    private final BurnBsqTab3View view;
    private final ChatUserService chatUserService;
    private Pin selectedUserProfilePin;

    public BurnBsqTab3Controller(DefaultApplicationService applicationService) {
        chatUserService = applicationService.getSocialService().getChatUserService();
        UserProfileSelection userProfileSelection = new UserProfileSelection(chatUserService);

        model = new BurnBsqTab3Model();
        view = new BurnBsqTab3View(model, this, userProfileSelection.getRoot());
    }

    @Override
    public void onActivate() {
        selectedUserProfilePin = FxBindings.subscribe(chatUserService.getSelectedChatUserIdentity(),
                chatUserIdentity -> {
                    model.getSelectedChatUserIdentity().set(chatUserIdentity);
                    model.getPubKeyHash().set(chatUserIdentity.getChatUser().getId());
                }
        );
    }

    @Override
    public void onDeactivate() {
        selectedUserProfilePin.unbind();
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.BURN_BSQ_TAB_2);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/reputation/burnBsq");
    }

    void onCopyToClipboard(String pubKeyHash) {
        ClipboardUtil.copyToClipboard(pubKeyHash);
    }

    void onClose() {
        OverlayController.hide();
    }
}
