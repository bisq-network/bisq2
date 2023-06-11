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

package bisq.desktop.primary.main.content.trade.multisig.old.takeOffer;

import bisq.account.accounts.Account;
import bisq.account.payment.Payment;
import bisq.account.protocol_type.ProtocolType;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.poc.PocOffer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class TakeOfferModel implements Model {
    Direction direction;
    PocOffer offer;
    Monetary baseSideAmount;
    Monetary quoteSideAmount;
    Quote fixPrice;
    private ProtocolType selectedProtocolType;

    private Account<?, ? extends Payment<?>> selectedBaseSideAccount;
    private Account<?, ? extends Payment<?>> selectedQuoteSideAccount;
    private Payment.Method selectedBaseSideSettlementMethod;
    private Payment.Method selectedQuoteSideSettlementMethod;

    final BooleanProperty createOfferButtonVisibleProperty = new SimpleBooleanProperty(true);
    BooleanProperty showTakeOfferTab;
}
