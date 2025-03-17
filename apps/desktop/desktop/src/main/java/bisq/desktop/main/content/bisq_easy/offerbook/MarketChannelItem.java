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

import bisq.bisq_easy.BisqEasySellersReputationBasedTradeAmountService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.FavouriteMarketsService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.effect.ColorAdjust;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class MarketChannelItem {
    public static final ColorAdjust DIMMED = new ColorAdjust(0, -0.2, -0.4, -0.1);
    public static final ColorAdjust SELECTED = new ColorAdjust(0, 0, -0.1, 0);
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    public static final String ASTERISK_SYMBOL = "\u002A"; // Unicode for "＊"

    @EqualsAndHashCode.Include
    private final BisqEasyOfferbookChannel channel;

    private final FavouriteMarketsService favouriteMarketsService;
    private final ChatNotificationService chatNotificationService;
    private final Market market;
    private final MarketPriceService marketPriceService;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    private final BisqEasySellersReputationBasedTradeAmountService bisqEasySellersReputationBasedTradeAmountService;
    private final SimpleIntegerProperty numOffers = new SimpleIntegerProperty(0);
    private final SimpleBooleanProperty isFavourite = new SimpleBooleanProperty(false);
    private final SimpleStringProperty numMarketNotifications = new SimpleStringProperty();
    private Pin channelPin;

    MarketChannelItem(BisqEasyOfferbookChannel channel,
                      FavouriteMarketsService favouriteMarketsService,
                      ChatNotificationService chatNotificationService,
                      MarketPriceService marketPriceService,
                      UserProfileService userProfileService,
                      ReputationService reputationService,
                      BisqEasySellersReputationBasedTradeAmountService bisqEasySellersReputationBasedTradeAmountService) {
        this.channel = channel;

        this.favouriteMarketsService = favouriteMarketsService;
        this.chatNotificationService = chatNotificationService;
        market = channel.getMarket();
        this.marketPriceService = marketPriceService;
        this.userProfileService = userProfileService;
        this.reputationService = reputationService;
        this.bisqEasySellersReputationBasedTradeAmountService = bisqEasySellersReputationBasedTradeAmountService;

        refreshNotifications();
        initialize();
    }

    private void initialize() {
        channelPin = channel.getChatMessages().addObserver(this::updateNumOffers);
    }

    public void dispose() {
        channelPin.unbind();
    }

    void refreshNotifications() {
        long numNotifications = chatNotificationService.getNumNotifications(channel);
        String value = "";
        if (numNotifications > 9) {
            // We don't have enough space for 2-digit numbers, so we show an asterix. Standard asterix would not be
            // centered, thus we use the `full width asterisk` taken from https://www.piliapp.com/symbol/asterisk/
            value = ASTERISK_SYMBOL;
        } else if (numNotifications > 0) {
            value = String.valueOf(numNotifications);
        }
        numMarketNotifications.set(value);
    }

    private void updateNumOffers() {
        UIThread.run(() -> {
            int numOffers = (int) channel.getChatMessages().stream()
                    .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                    .filter(bisqEasySellersReputationBasedTradeAmountService::hasSellerSufficientReputation)
                    .count();
            getNumOffers().set(numOffers);
        });
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
