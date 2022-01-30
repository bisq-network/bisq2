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

package bisq.desktop.primary.main.content.trade.take;

import bisq.account.accounts.Account;
import bisq.account.protocol.SwapProtocolType;
import bisq.account.settlement.SettlementMethod;
import bisq.common.monetary.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.Offer;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class TakeOfferModel implements Model {
    public ObjectProperty<SwapProtocolType> selectedProtocolTypeProperty = new SimpleObjectProperty<>(); //todo

    ObjectProperty<Market> selectedMarketProperty = new SimpleObjectProperty<>();
    ReadOnlyObjectProperty<Direction> directionProperty;
   // SwapProtocolType selectedProtocol;

    Offer offer;
    Monetary baseSideAmount;
    Monetary quoteSideAmount;
    Quote fixPrice;

    private ReadOnlyObjectProperty<Account<? extends SettlementMethod>> selectedBaseSideAccount;
    private ReadOnlyObjectProperty<Account<? extends SettlementMethod>> selectedQuoteSideAccount;
    private ReadOnlyObjectProperty<SettlementMethod> selectedBaseSideSettlementMethod;
    private ReadOnlyObjectProperty<SettlementMethod> selectedQuoteSideSettlementMethod;
    
    final BooleanProperty createOfferButtonVisibleProperty = new SimpleBooleanProperty(true);
    BooleanProperty showTakeOfferTab;

    public TakeOfferModel() {
    }
}
