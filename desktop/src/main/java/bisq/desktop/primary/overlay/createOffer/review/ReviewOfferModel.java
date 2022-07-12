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

package bisq.desktop.primary.overlay.createOffer.review;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.offer.spec.Direction;
import bisq.chat.channel.trade.pub.PublicTradeChannel;
import bisq.chat.message.PublicTradeChatMessage;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
class ReviewOfferModel implements Model {
    @Setter
    private PublicTradeChannel selectedChannel;
    @Setter
    private Direction direction = Direction.BUY;
    @Setter
    private Market market = MarketRepository.getDefault();
    @Setter
    private Monetary baseSideAmount = Coin.asBtc(10000);
    @Setter
    private Monetary quoteSideAmount = Fiat.parse("100", "EUR");
    @Setter
    private List<String> paymentMethods = List.of("SEPA");
    final private ObjectProperty<PublicTradeChatMessage> myOfferMessage = new SimpleObjectProperty<>();
    final private BooleanProperty matchingOffersFound = new SimpleBooleanProperty();
    private final BooleanProperty showCreateOfferSuccess = new SimpleBooleanProperty();
    private final BooleanProperty showTakeOfferSuccess = new SimpleBooleanProperty();

    ReviewOfferModel() {
    }
}