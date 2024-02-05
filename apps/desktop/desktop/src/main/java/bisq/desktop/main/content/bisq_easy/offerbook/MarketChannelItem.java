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
import bisq.common.currency.Market;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.CacheHint;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.lang.ref.WeakReference;

@EqualsAndHashCode
@Getter
public class MarketChannelItem {
    private final BisqEasyOfferbookChannel channel;
    private final Market market;
    private final ImageView marketLogo;
    private final IntegerProperty numOffers = new SimpleIntegerProperty(0);

    public MarketChannelItem(BisqEasyOfferbookChannel channel) {
        this.channel = channel;
        market = channel.getMarket();

        String marketCode = market.getQuoteCurrencyCode().toLowerCase();
        String iconId = String.format("market-%s", marketCode);
        marketLogo = ImageUtil.getImageViewById(iconId);
        marketLogo.setCache(true);
        marketLogo.setCacheHint(CacheHint.SPEED);
        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(-0.1);
        marketLogo.setEffect(colorAdjust);

        channel.getChatMessages().addObserver(new WeakReference<Runnable>(this::updateNumOffers).get());
        updateNumOffers();
    }

    private void updateNumOffers() {
        UIThread.run(() -> {
            int numOffers = (int) channel.getChatMessages().stream()
                    .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                    .count();
            this.getNumOffers().set(numOffers);
        });
    }

    public String getMarketString() {
        return market.toString();
    }

    @Override
    public String toString() {
        return market.toString();
    }
}
