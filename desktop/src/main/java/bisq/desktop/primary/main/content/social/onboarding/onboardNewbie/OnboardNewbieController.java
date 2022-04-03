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
import bisq.presentation.formatters.AmountFormatter;
import bisq.social.chat.ChatService;
import bisq.social.chat.PublicChannel;
import bisq.social.intent.TradeIntent;
import bisq.social.intent.TradeIntentService;
import bisq.social.intent.TradeIntentStore;
import bisq.social.user.profile.UserProfile;
import com.google.common.base.Joiner;
import javafx.beans.InvalidationListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.text.BreakIterator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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
    private final InvalidationListener paymentMethodsSelectionListener;

    public OnboardNewbieController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        tradeIntentService = applicationService.getTradeIntentService();
        model = new OnboardNewbieModel(applicationService.getUserProfileService().getPersistableStore().getSelectedUserProfile().get().userName());

        marketSelection = new MarketSelection(applicationService.getSettingsService());
        btcFiatAmountGroup = new BtcFiatAmountGroup(applicationService.getMarketPriceService());

        paymentMethodsSelection = new PaymentMethodsSelection(applicationService.getTradeIntentService());

        view = new OnboardNewbieView(model, this,
                marketSelection.getRoot(),
                btcFiatAmountGroup.getRoot(),
                paymentMethodsSelection);

        Direction direction = Direction.BUY;
        btcFiatAmountGroup.setDirection(direction);
        paymentMethodsSelection.setDirection(direction);

        paymentMethodsSelectionListener = o -> {
            model.getSelectedPaymentMethods().setAll(paymentMethodsSelection.getSelectedPaymentMethods());
            updateOfferPreview();
        };
    }


    @Override
    public void onActivate() {
        selectedMarketSubscription = EasyBind.subscribe(marketSelection.selectedMarketProperty(),
                selectedMarket -> {
                    model.setSelectedMarket(selectedMarket);
                    btcFiatAmountGroup.setSelectedMarket(selectedMarket);
                    paymentMethodsSelection.setSelectedMarket(selectedMarket);
                    updateOfferPreview();
                });
        baseSideAmountSubscription = EasyBind.subscribe(btcFiatAmountGroup.baseSideAmountProperty(),
                e -> {
                    model.setBaseSideAmount(e);
                    updateOfferPreview();
                });

        TradeIntentStore tradeIntentStore = tradeIntentService.getPersistableStore();
        tradeTagsPin = FxBindings.<String, String>bind(model.getTradeTags()).map(String::toUpperCase).to(tradeIntentStore.getTradeTags());
        currencyTagsPin = FxBindings.<String, String>bind(model.getCurrencyTags()).map(String::toUpperCase).to(tradeIntentStore.getCurrencyTags());
        paymentMethodTagsPin = FxBindings.<String, String>bind(model.getPaymentMethodsTags()).map(String::toUpperCase).to(tradeIntentStore.getPaymentMethodTags());

        paymentMethodsSelection.getSelectedPaymentMethods().addListener(paymentMethodsSelectionListener);

        model.getSelectedPaymentMethods().setAll(paymentMethodsSelection.getSelectedPaymentMethods());
        model.getTerms().set(Res.get("satoshisquareapp.createOffer.defaultTerms"));

        updateOfferPreview();
    }

    @Override
    public void onDeactivate() {
        selectedMarketSubscription.unsubscribe();
        baseSideAmountSubscription.unsubscribe();
        tradeTagsPin.unbind();
        currencyTagsPin.unbind();
        paymentMethodTagsPin.unbind();
        paymentMethodsSelection.getSelectedPaymentMethods().removeListener(paymentMethodsSelectionListener);
    }

    void onCreateOffer() {
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

        TradeIntent tradeIntent = new TradeIntent(model.getBaseSideAmount().getValue(),
                model.getSelectedMarket().quoteCurrencyCode(),
                new HashSet<>(model.getSelectedPaymentMethods()));
        chatService.publishTradeChatMessage(tradeIntent, publicChannel, userProfile);
        Navigation.navigateTo(NavigationTarget.CHAT);
    }


    void onSkip() {
        Navigation.navigateTo(NavigationTarget.CHAT);
    }

    private void updateOfferPreview() {
        boolean isInvalidTradeIntent = isInvalidTradeIntent();
        model.getIsInvalidTradeIntent().set(isInvalidTradeIntent);
        if (isInvalidTradeIntent) {
            return;
        }

        String amount = AmountFormatter.formatAmountWithCode(model.getBaseSideAmount(), true);
        String quoteCurrency = model.getSelectedMarket().quoteCurrencyCode();
        String paymentMethods = Joiner.on(", ").join(model.getSelectedPaymentMethods());

        String previewText = Res.get("satoshisquareapp.createOffer.offerPreview", amount, quoteCurrency, paymentMethods);
        model.getStyleSpans().set(getStyleSpans(previewText,
                model.getTradeTags(),
                model.getCurrencyTags(),
                model.getPaymentMethodsTags()));
        model.getOfferPreview().set(previewText);
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
            if (lastIndex != BreakIterator.DONE) {
                String word = text.substring(firstIndex, lastIndex).toUpperCase();
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
                } else if (word.equals("BTC") || word.equals("BITCOIN")) {
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
        return spansBuilder.create();
    }

    private boolean isInvalidTradeIntent() {
        return model.getBaseSideAmount() == null ||
                model.getSelectedMarket() == null ||
                model.getSelectedPaymentMethods() == null ||
                model.getSelectedPaymentMethods().isEmpty();
    }
}
