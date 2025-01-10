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

package bisq.desktop.main.content.user.profile_card.messages;

import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.bisq_easy.offerbook.offerbook_list.OfferbookListItem;
import bisq.user.profile.UserProfile;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ProfileCardMessagesController implements Controller {
    @Getter
    private final ProfileCardMessagesView view;
    private final ProfileCardMessagesModel model;

    public ProfileCardMessagesController(ServiceProvider serviceProvider) {
        model = new ProfileCardMessagesModel();
        view = new ProfileCardMessagesView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void updateUserProfileData(UserProfile userProfile) {
    }
}
