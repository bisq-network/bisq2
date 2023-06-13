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

package bisq.desktop.primary.main.content.trade.poc.old.createOffer;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.common.view.Model;
import bisq.offer.Direction;
import bisq.offer.poc.PocOffer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

@Slf4j
@Getter
public class MultiSigCreateOfferModel implements Model {
    @Setter
    private Market selectedMarket;
    @Setter
    private Direction direction;
    @Setter
    private TradeProtocolType selectedProtocolType;
    @Setter
    private Monetary baseSideAmount;
    @Setter
    private Monetary quoteSideAmount;
    @Setter
    private PriceQuote fixPrice;

    private final ObservableSet<Account<?, ? extends PaymentMethod<?>>> selectedBaseSideAccounts = FXCollections.observableSet(new HashSet<>());
    private final ObservableSet<Account<?, ? extends PaymentMethod<?>>> selectedQuoteSideAccounts = FXCollections.observableSet(new HashSet<>());
    private final ObservableSet<PaymentRail> selectedBaseSidePaymentPaymentRails = FXCollections.observableSet(new HashSet<>());
    private final ObservableSet<PaymentRail> selectedQuoteSidePaymentPaymentRails = FXCollections.observableSet(new HashSet<>());

    private final ObjectProperty<PocOffer> offerProperty = new SimpleObjectProperty<>();
    private final BooleanProperty createOfferButtonVisibleProperty = new SimpleBooleanProperty(true);
    @Setter
    private boolean showCreateOfferTab;

    public MultiSigCreateOfferModel() {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public PocOffer getOffer() {
        return offerProperty.get();
    }

    public ReadOnlyBooleanProperty createOfferButtonVisibleProperty() {
        return createOfferButtonVisibleProperty;
    }

    public void setAllSelectedBaseSideAccounts(ObservableSet<Account<?, ? extends PaymentMethod<?>>> set) {
        selectedBaseSideAccounts.clear();
        selectedBaseSideAccounts.addAll(set);
    }

    public void setAllSelectedQuoteSideAccounts(ObservableSet<Account<?, ? extends PaymentMethod<?>>> set) {
        selectedQuoteSideAccounts.clear();
        selectedQuoteSideAccounts.addAll(set);
    }

    public void setAllSelectedBaseSidePaymentMethods(ObservableSet<PaymentRail> set) {
        selectedBaseSidePaymentPaymentRails.clear();
        selectedBaseSidePaymentPaymentRails.addAll(set);
    }

    public void setAllSelectedQuoteSidePaymentMethods(ObservableSet<PaymentRail> set) {
        selectedQuoteSidePaymentPaymentRails.clear();
        selectedQuoteSidePaymentPaymentRails.addAll(set);
    }
}
