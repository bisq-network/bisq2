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

package bisq.desktop.primary.main.content.settings.reputation.earnReputation.bond;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.Browser;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.components.UserProfileSelection;
import bisq.desktop.primary.main.content.settings.reputation.burn.ReputationSourceListItem;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.social.user.ChatUserService;
import bisq.social.user.reputation.Reputation;
import lombok.Getter;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BsqBondController implements Controller {

    private final BsqBondModel model;
    @Getter
    private final BsqBondView view;
    private final ChatUserService chatUserService;
    private Pin selectedUserProfilePin;

    public BsqBondController(DefaultApplicationService applicationService) {
        chatUserService = applicationService.getChatUserService();
        UserProfileSelection userProfileSelection = new UserProfileSelection(chatUserService);

        model = new BsqBondModel();
        view = new BsqBondView(model, this, userProfileSelection.getRoot());
        model.getSources().setAll(Stream.of(Reputation.Source.values())
                .filter(e -> e != Reputation.Source.PROFILE_AGE)
                .map(ReputationSourceListItem::new)
                .collect(Collectors.toList()));
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

    public void onSelectSource(ReputationSourceListItem selected) {
        model.getSelectedSource().set(selected);
    }

    void onNext() {
    }

    void onClose() {
        OverlayController.hide();
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/reputation");
    }
}
