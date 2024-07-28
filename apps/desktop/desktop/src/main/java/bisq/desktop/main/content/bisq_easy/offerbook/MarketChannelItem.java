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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import bisq.settings.FavouriteMarketsService;
import javafx.beans.property.*;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class MarketChannelItem {
    private static final ColorAdjust DEFAULT_COLOR_ADJUST = new ColorAdjust();
    private static final ColorAdjust SELECTED_COLOR_ADJUST = new ColorAdjust();

    @EqualsAndHashCode.Include
    private final BisqEasyOfferbookChannel channel;
    private final FavouriteMarketsService favouriteMarketsService;
    private final ChatNotificationService chatNotificationService;
    private final Market market;
    private final Node marketLogo;
    private final IntegerProperty numOffers = new SimpleIntegerProperty(0);
    private final BooleanProperty isFavourite = new SimpleBooleanProperty(false);
    private final StringProperty numMarketNotifications = new SimpleStringProperty();
    private Pin changedChatNotificationPin;

    MarketChannelItem(BisqEasyOfferbookChannel channel,
                      FavouriteMarketsService favouriteMarketsService,
                      ChatNotificationService chatNotificationService) {
        this.channel = channel;
        this.favouriteMarketsService = favouriteMarketsService;
        this.chatNotificationService = chatNotificationService;
        market = channel.getMarket();
        marketLogo = MarketImageComposition.createMarketLogo(market.getQuoteCurrencyCode());
        marketLogo.setCache(true);
        marketLogo.setCacheHint(CacheHint.SPEED);

        setUpColorAdjustments();
        marketLogo.setEffect(DEFAULT_COLOR_ADJUST);

        channel.getChatMessages().addObserver(new WeakReference<Runnable>(this::updateNumOffers).get());
        updateNumOffers();

        chatNotificationService.getNotConsumedNotifications(channel).forEach(this::applyNotification);

        onActivate();
    }

    void onActivate() {
        if (changedChatNotificationPin != null) {
            changedChatNotificationPin.unbind();
        }
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(notification ->
                UIThread.run(() -> applyNotification(notification)));
    }

    void onDeactivate() {
        if (changedChatNotificationPin != null) {
            changedChatNotificationPin.unbind();
        }
    }

    private void applyNotification(ChatNotification notification) {
        if (notification == null) {
            return;
        }
        long numNotifications = chatNotificationService.getNumNotifications(channel);
        String value = "";
        if (numNotifications > 9) {
            // We don't have enough space for 2-digit numbers, so we show an asterix. Standard asterix would not be
            // centered, thus we use the `full width asterisk` taken from https://www.piliapp.com/symbol/asterisk/
            value = "＊";
        } else if (numNotifications > 0) {
            value = String.valueOf(numNotifications);
        }
        numMarketNotifications.set(value);
    }

    private void setUpColorAdjustments() {
        DEFAULT_COLOR_ADJUST.setBrightness(-0.4);
        DEFAULT_COLOR_ADJUST.setSaturation(-0.2);
        DEFAULT_COLOR_ADJUST.setContrast(-0.1);

        SELECTED_COLOR_ADJUST.setBrightness(-0.1);
    }

    private void updateNumOffers() {
        UIThread.run(() -> {
            int numOffers = (int) channel.getChatMessages().stream()
                    .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                    .count();
            getNumOffers().set(numOffers);
        });
    }

    void updateMarketLogoEffect(boolean isSelectedMarket) {
        getMarketLogo().setEffect(isSelectedMarket ? SELECTED_COLOR_ADJUST : DEFAULT_COLOR_ADJUST);
    }

    @Override
    public String toString() {
        return market.toString();
    }

    void toggleFavourite() {
        if (isFavourite()) {
            removeFromFavourites();
        } else {
            addAsFavourite();
        }
    }

    private boolean isFavourite() {
        return favouriteMarketsService.isFavourite(getMarket());
    }

    private void addAsFavourite() {
        if (!favouriteMarketsService.canAddNewFavourite()) {
            new Popup().information(Res.get("bisqEasy.offerbook.marketListCell.favourites.maxReached.popup"))
                    .closeButtonText(Res.get("confirmation.ok"))
                    .show();
            return;
        }

        favouriteMarketsService.addFavourite(getMarket());
    }

    private void removeFromFavourites() {
        favouriteMarketsService.removeFavourite(getMarket());
    }
}
