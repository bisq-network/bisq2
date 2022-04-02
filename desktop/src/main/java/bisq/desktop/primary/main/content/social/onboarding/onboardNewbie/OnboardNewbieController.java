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

package bisq.desktop.primary.main.content.social.onboarding.onboardNewbie;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.ObservableSet;
import bisq.common.observable.Pin;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.trade.components.MarketSelection;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import bisq.social.chat.ChatService;
import bisq.social.chat.PublicChannel;
import bisq.social.intent.TradeIntentService;
import bisq.social.intent.TradeIntentStore;
import bisq.social.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.text.BreakIterator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class OnboardNewbieController implements Controller {
    private final TradeIntentService tradeIntentService;
    private final OnboardNewbieModel model;
    @Getter
    private final OnboardNewbieView view;
    private final MarketSelection marketSelection;
    private final BtcFiatAmountGroup btcFiatAmountGroup;
    private final DefaultApplicationService applicationService;
    private final PaymentMethodsSelection paymentMethodsSelection;
    private Pin tradeTagsPin, currencyTagsPin, paymentMethodTagsPin;

    private Subscription selectedMarketSubscription, baseSideAmountSubscription;

    public OnboardNewbieController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        tradeIntentService = applicationService.getTradeIntentService();
        model = new OnboardNewbieModel(applicationService.getUserProfileService().getPersistableStore().getSelectedUserProfile().get().userName());

        marketSelection = new MarketSelection(applicationService.getSettingsService());
        btcFiatAmountGroup = new BtcFiatAmountGroup(applicationService.getMarketPriceService());

        paymentMethodsSelection = new PaymentMethodsSelection();

        view = new OnboardNewbieView(model, this,
                marketSelection.getRoot(),
                btcFiatAmountGroup.getRoot(),
                paymentMethodsSelection);

        Direction direction = Direction.BUY;
        btcFiatAmountGroup.setDirection(direction);
        paymentMethodsSelection.setDirection(direction);
    }


    @Override
    public void onActivate() {
        selectedMarketSubscription = EasyBind.subscribe(marketSelection.selectedMarketProperty(),
                selectedMarket -> {
                    model.setSelectedMarket(selectedMarket);
                    btcFiatAmountGroup.setSelectedMarket(selectedMarket);
                    paymentMethodsSelection.setSelectedMarket(selectedMarket);
                });
        baseSideAmountSubscription = EasyBind.subscribe(btcFiatAmountGroup.baseSideAmountProperty(),
                model::setBaseSideAmount);

        model.getSelectedPaymentMethods().setAll(paymentMethodsSelection.getSelectedPaymentMethods());

        model.getTerms().set(Res.get("satoshisquareapp.createOffer.defaultTerms"));


        TradeIntentStore tradeIntentStore = tradeIntentService.getPersistableStore();
        tradeTagsPin = FxBindings.<String, String>bind(model.getTradeTags()).to(tradeIntentStore.getTradeTags());
        currencyTagsPin = FxBindings.<String, String>bind(model.getCurrencyTags()).to(tradeIntentStore.getCurrencyTags());
        paymentMethodTagsPin = FxBindings.<String, String>bind(model.getPaymentMethodTags()).to(tradeIntentStore.getPaymentMethodTags());
        updateOfferPreview();
    }

    @Override
    public void onDeactivate() {
        selectedMarketSubscription.unsubscribe();
        baseSideAmountSubscription.unsubscribe();
        tradeTagsPin.unbind();
        currencyTagsPin.unbind();
        paymentMethodTagsPin.unbind();
    }

    private void updateOfferPreview() {
        //todo
        String amount = "0.007 BTC";
        String quoteCurrency = "EUR";
        String paymentMethods = "SEPA, Bank-transfer";

        String previewText = Res.get("satoshisquareapp.createOffer.offerPreview",
                amount, quoteCurrency, paymentMethods);
        model.getStyleSpans().set(getStyleSpans(previewText, model.getTradeTags(), model.getCurrencyTags(), model.getPaymentMethodTags()));
        model.getOfferPreview().set(previewText);
    }

    public void onCreateOffer() {
        ChatService chatService = applicationService.getChatService();
        ObservableSet<PublicChannel> publicChannels = chatService.getPersistableStore().getPublicChannels();
        PublicChannel publicChannel = publicChannels.stream()
                .filter(e -> e.getChannelName().toLowerCase().contains(model.getSelectedMarket().quoteCurrencyCode().toLowerCase()))
                .findAny()
                .orElse(publicChannels.stream()
                        .filter(e -> e.getChannelName().toLowerCase().contains("other"))
                        .findAny()
                        .orElseThrow());
        UserProfile userProfile = applicationService.getUserProfileService().getPersistableStore().getSelectedUserProfile().get();
        
        //todo add tradeIntent object
        chatService.publishPublicChatMessage(model.getOfferPreview().get(),
                Optional.empty(),
                publicChannel,
                userProfile);
        Navigation.navigateTo(NavigationTarget.CHAT);
    }


    public void onSkip() {
        Navigation.navigateTo(NavigationTarget.CHAT);
    }

    private StyleSpans<Collection<String>> getStyleSpans(String text,
                                                         List<String> tradeTags,
                                                         List<String> currencyTags,
                                                         List<String> paymentMethodTags) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        BreakIterator iterator = BreakIterator.getWordInstance();
        iterator.setText(text);
        int lastIndex = iterator.first();
        int lastKwEnd = 0;
        while (lastIndex != BreakIterator.DONE) {
            int firstIndex = lastIndex;
            lastIndex = iterator.next();
            if (lastIndex != BreakIterator.DONE && Character.isLetterOrDigit(text.charAt(firstIndex))) {
                String word = text.substring(firstIndex, lastIndex).toLowerCase();
                if (tradeTags.contains(word)) {
                    spansBuilder.add(Collections.emptyList(), firstIndex - lastKwEnd);
                    spansBuilder.add(Collections.singleton("keyword-tradeTags"), lastIndex - firstIndex);
                    lastKwEnd = lastIndex;
                } else if (currencyTags.contains(word)) {
                    spansBuilder.add(Collections.emptyList(), firstIndex - lastKwEnd);
                    spansBuilder.add(Collections.singleton("keyword-currencyTags"), lastIndex - firstIndex);
                    lastKwEnd = lastIndex;
                } else if (paymentMethodTags.contains(word)) {
                    spansBuilder.add(Collections.emptyList(), firstIndex - lastKwEnd);
                    spansBuilder.add(Collections.singleton("keyword-paymentMethodTags"), lastIndex - firstIndex);
                    lastKwEnd = lastIndex;
                } else if (word.equals("btc") || word.equals("bitcoin")) {
                    // I would like to buy 0.007 BTC for EUR using SEPA, Bank transfers
                    spansBuilder.add(Collections.emptyList(), firstIndex - lastKwEnd);
                    spansBuilder.add(Collections.singleton("keyword-btc"), lastIndex - firstIndex);
                    lastKwEnd = lastIndex;
                } else if (word.matches("[0-9]{1,13}(\\.[0-9]*)?")) {
                    spansBuilder.add(Collections.emptyList(), firstIndex - lastKwEnd);
                    spansBuilder.add(Collections.singleton("keyword-amountTag"), lastIndex - firstIndex);
                    lastKwEnd = lastIndex;
                } else {
                    spansBuilder.add(Collections.emptyList(), firstIndex - lastKwEnd);
                    spansBuilder.add(Collections.singleton("keyword-none"), lastIndex - firstIndex);
                    lastKwEnd = lastIndex;
                }
            }
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        StyleSpans<Collection<String>> styleSpans = spansBuilder.create();
        return styleSpans;
    }
}
