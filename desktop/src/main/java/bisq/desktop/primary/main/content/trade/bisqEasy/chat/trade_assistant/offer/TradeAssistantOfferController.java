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
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.offer_details.BisqEasyOfferDetailsController;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.security.KeyPairService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeAssistantOfferController implements Controller {
    @Getter
    private final TradeAssistantOfferView view;
    private final TradeAssistantOfferModel model;
    private final KeyPairService keyPairService;

    public TradeAssistantOfferController(DefaultApplicationService applicationService) {
        keyPairService = applicationService.getSecurityService().getKeyPairService();

        model = new TradeAssistantOfferModel();
        view = new TradeAssistantOfferView(model, this);
    }

    public void setBisqEasyOffer(BisqEasyOffer bisqEasyOffer) {
        model.setBisqEasyOffer(bisqEasyOffer);
        String makersDirection = bisqEasyOffer.getDirectionAsDisplayString();
        String takersDirection = bisqEasyOffer.getMirroredDirectionAsDisplayString();
        String headline = isMyOffer(bisqEasyOffer) ? Res.get("tradeAssistant.offer.maker.headline", makersDirection) :
                Res.get("tradeAssistant.offer.taker.headline", takersDirection);
        model.getHeadline().set(headline);

        String fiat = bisqEasyOffer.getQuoteSideAmountAsDisplayString();
        String settlementMethods = bisqEasyOffer.getSettlementMethodsAsDisplayString();
        model.getOfferInfo().set(Res.get("tradeAssistant.offer.info", makersDirection, fiat, settlementMethods));
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

    private boolean isMyOffer(BisqEasyOffer bisqEasyOffer) {
        return keyPairService.findKeyPair(bisqEasyOffer.getMakerNetworkId().getPubKey().getKeyId()).isPresent();
    }
}
