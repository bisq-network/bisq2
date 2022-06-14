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

package bisq.desktop.primary.overlay.onboarding.offer.published;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.social.chat.ChatService;
import bisq.social.chat.messages.PublicTradeChatMessage;
import bisq.social.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OfferPublishedController implements Controller {
    private final OfferPublishedModel model;
    @Getter
    private final OfferPublishedView view;
   // private final ChatMessagesListView myOfferListView;
    private final ChatService chatService;
    private final ReputationService reputationService;

    public OfferPublishedController(DefaultApplicationService applicationService) {
        chatService = applicationService.getChatService();
        reputationService = applicationService.getReputationService();
    /*    myOfferListView = new ChatMessagesListView(applicationService,
                mentionUser -> {
                },
                showChatUserDetails -> {
                },
                onReply -> {
                },
                false,
                true,
                false,
                true);*/

        model = new OfferPublishedModel();
        view = new OfferPublishedView(model, this/*, myOfferListView.getRoot()*/);
    }

    @Override
    public void onActivate() {
      /*  myOfferListView.getFilteredChatMessages().setPredicate(item -> item.getChatMessage().equals(model.getMyOfferMessage()));
        myOfferListView.getChatMessages().clear();
        myOfferListView.getChatMessages().add(new ChatMessagesListView.ChatMessageListItem<>(model.getMyOfferMessage(), chatService, reputationService));*/
    }

    @Override
    public void onDeactivate() {
    }

    public void setMyOfferMessage(PublicTradeChatMessage myOfferMessage) {
        if (myOfferMessage == null) {
            return;
        }
        model.setMyOfferMessage(myOfferMessage);
      /*  myOfferListView.getChatMessages().clear();
        myOfferListView.getChatMessages().add(new ChatMessagesListView.ChatMessageListItem<>(model.getMyOfferMessage(), chatService, reputationService));*/
    }
}
