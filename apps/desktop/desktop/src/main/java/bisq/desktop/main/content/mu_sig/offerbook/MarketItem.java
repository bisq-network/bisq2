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

package bisq.desktop.main.content.mu_sig.offerbook;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigService;
import bisq.settings.FavouriteMarketsService;
import bisq.user.profile.UserProfileService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.effect.ColorAdjust;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class MarketItem {
    public static final ColorAdjust DIMMED = new ColorAdjust(0, -0.2, -0.4, -0.1);
    public static final ColorAdjust SELECTED = new ColorAdjust(0, 0, -0.1, 0);
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    public static final String ASTERISK_SYMBOL = "\u002A"; // Unicode for "＊"

    @EqualsAndHashCode.Include
    private final Market market;

    private final FavouriteMarketsService favouriteMarketsService;
    private final MarketPriceService marketPriceService;
    private final UserProfileService userProfileService;
    private final MuSigService muSigService;
    private final SimpleLongProperty numOffers = new SimpleLongProperty(0);
    private final SimpleBooleanProperty isFavourite = new SimpleBooleanProperty(false);
    private final SimpleStringProperty numMarketNotifications = new SimpleStringProperty();
    private Pin offersPin;

    MarketItem(Market market,
               FavouriteMarketsService favouriteMarketsService,
               MarketPriceService marketPriceService,
               UserProfileService userProfileService,
               MuSigService muSigService) {
        this.market = market;
        this.favouriteMarketsService = favouriteMarketsService;
        this.marketPriceService = marketPriceService;
        this.userProfileService = userProfileService;
        this.muSigService = muSigService;

//        refreshNotifications();
        initialize();
    }

    private void initialize() {
        offersPin = muSigService.getObservableOffers().addObserver(this::updateNumOffers);
    }

    public void dispose() {
        offersPin.unbind();
    }

//    void refreshNotifications() {
//        long numNotifications = chatNotificationService.getNumNotifications(channel);
//        String value = "";
//        if (numNotifications > 9) {
//            // We don't have enough space for 2-digit numbers, so we show an asterix. Standard asterix would not be
//            // centered, thus we use the `full width asterisk` taken from https://www.piliapp.com/symbol/asterisk/
//            value = ASTERISK_SYMBOL;
//        } else if (numNotifications > 0) {
//            value = String.valueOf(numNotifications);
//        }
//        numMarketNotifications.set(value);
//    }

    private void updateNumOffers() {
        UIThread.run(() -> {
            long numOffers = muSigService.getOffers().stream()
                    .filter(offer -> {
                        Market offerMarket = offer.getMarket();

                        // TODO: Needs to be dynamic according to base market
                        // for now we just assume Btc.
                        boolean isBaseMarket = offerMarket.isBtcFiatMarket() && offerMarket.getBaseCurrencyCode().equals("BTC");
                        boolean isQuoteMarket = offerMarket.getQuoteCurrencyCode().equals(market.getQuoteCurrencyCode());
                        return isBaseMarket && isQuoteMarket;
                    })
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
