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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.offer;

import bisq.application.DefaultApplicationService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.offer_details.BisqEasyOfferDetailsController;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.security.KeyPairService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class TradeAssistantOfferController implements Controller {
    @Getter
    private final TradeAssistantOfferView view;
    private final TradeAssistantOfferModel model;
    private final KeyPairService keyPairService;
    private final Consumer<UserProfile> openUserProfileSidebarHandler;

    public TradeAssistantOfferController(DefaultApplicationService applicationService, Consumer<UserProfile> openUserProfileSidebarHandler) {
        keyPairService = applicationService.getSecurityService().getKeyPairService();
        this.openUserProfileSidebarHandler = openUserProfileSidebarHandler;

        model = new TradeAssistantOfferModel();
        view = new TradeAssistantOfferView(model, this);
    }

    public void setBisqEasyPrivateTradeChatChannel(BisqEasyPrivateTradeChatChannel privateChannel) {
        BisqEasyOffer bisqEasyOffer = privateChannel.getBisqEasyOffer();
        model.setBisqEasyOffer(bisqEasyOffer);
        String makersDirection = bisqEasyOffer.getDirectionAsDisplayString();
        String takersDirection = bisqEasyOffer.getMirroredDirectionAsDisplayString();

        UserProfile peersUserProfile = privateChannel.getPeer();
        model.setPeersUserProfile(peersUserProfile);
        String peersUserName = peersUserProfile.getUserName();
        if (isMyOffer(bisqEasyOffer)) {
            model.getHeadline().set(Res.get("tradeAssistant.offer.maker.headline", makersDirection, peersUserName));
            model.getOfferTitle().set(Res.get("tradeAssistant.offer.maker.offerTitle"));
        } else {
            model.getHeadline().set(Res.get("tradeAssistant.offer.taker.headline", peersUserName, takersDirection));
            model.getOfferTitle().set(Res.get("tradeAssistant.offer.taker.offerTitle", peersUserName));
        }

        model.getAmount().set(bisqEasyOffer.getQuoteSideAmountAsDisplayString());
        model.getPaymentMethods().set(bisqEasyOffer.getSettlementMethodsAsDisplayString());

        model.getOpenUserProfileButtonLabel().set(Res.get("tradeAssistant.offer.peer.openUserProfile", peersUserName));
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.TRADE_ASSISTANT_NEGOTIATION);
    }

    void onOpenOfferDetails() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_OFFER_DETAILS, new BisqEasyOfferDetailsController.InitData(model.getBisqEasyOffer()));
    }

    void onOpenUserProfile() {
        openUserProfileSidebarHandler.accept(model.getPeersUserProfile());
    }

    private boolean isMyOffer(BisqEasyOffer bisqEasyOffer) {
        return keyPairService.findKeyPair(bisqEasyOffer.getMakerNetworkId().getPubKey().getKeyId()).isPresent();
    }

    void onOpenTradeGuide() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
    }
}
