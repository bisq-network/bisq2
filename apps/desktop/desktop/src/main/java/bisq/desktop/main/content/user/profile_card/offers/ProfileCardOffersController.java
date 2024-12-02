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

package bisq.desktop.main.content.user.profile_card.offers;

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.user.profile.UserProfile;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ProfileCardOffersController implements Controller {
    @Getter
    private final ProfileCardOffersView view;
    private final ProfileCardOffersModel model;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;

    public ProfileCardOffersController(ServiceProvider serviceProvider) {
        model = new ProfileCardOffersModel();
        view = new ProfileCardOffersView(model, this);
        bisqEasyOfferbookChannelService = serviceProvider.getChatService().getBisqEasyOfferbookChannelService();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void updateUserProfileData(UserProfile userProfile) {
        model.getListItems().clear();

        List<ProfileCardOffersView.ListItem> userOffers = new ArrayList<>();
        for (BisqEasyOfferbookChannel market : bisqEasyOfferbookChannelService.getChannels()) {
            userOffers.addAll(market.getChatMessages().stream()
                    .filter(chatMessage -> chatMessage.hasBisqEasyOffer()
                            && chatMessage.getAuthorUserProfileId().equals(userProfile.getId()))
                    .map(userChatMessageWithOffer -> new ProfileCardOffersView.ListItem(userProfile, userChatMessageWithOffer))
                    .toList());
        }
        model.getListItems().addAll(userOffers);
    }
}
