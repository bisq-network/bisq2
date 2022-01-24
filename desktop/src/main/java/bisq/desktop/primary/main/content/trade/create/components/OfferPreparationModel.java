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

package bisq.desktop.primary.main.content.trade.create.components;

import bisq.account.accounts.Account;
import bisq.account.protocol.SwapProtocolType;
import bisq.account.settlement.SettlementMethod;
import bisq.common.monetary.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.offer.Direction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

/**
 * Shared model for offer preparation
 * Used via Lombok Delegates in components
 * IDE does not recognize that and shows getters incorrectly as unused
 */
@Slf4j
public class OfferPreparationModel {
    private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();
    private final ObjectProperty<Direction> direction = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> baseSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> quoteSideAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<Quote> fixPrice = new SimpleObjectProperty<>();
    private final ObjectProperty<SwapProtocolType> selectedProtocolType = new SimpleObjectProperty<>();

    private final ObservableSet<Account<? extends SettlementMethod>> selectedBaseSideAccounts = FXCollections.observableSet(new HashSet<>());
    private final ObservableSet<Account<? extends SettlementMethod>> selectedQuoteSideAccounts = FXCollections.observableSet(new HashSet<>());
    private final ObservableSet<SettlementMethod> selectedBaseSideSettlementMethods = FXCollections.observableSet(new HashSet<>());
    private final ObservableSet<SettlementMethod> selectedQuoteSideSettlementMethods = FXCollections.observableSet(new HashSet<>());

    public OfferPreparationModel() {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Setters
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void setSelectedMarket(Market value) {
        selectedMarket.set(value);
    }

    public void setDirection(Direction value) {
        direction.set(value);
    }

    public void setBaseSideAmount(Monetary value) {
        baseSideAmount.set(value);
    }

    public void setFixPrice(Quote value) {
        fixPrice.set(value);
    }

    public void setQuoteSideAmount(Monetary value) {
        quoteSideAmount.set(value);
    }

    public void setSelectedProtocolType(SwapProtocolType value) {
        selectedProtocolType.set(value);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Market getSelectedMarket() {
        return selectedMarket.get();
    }

    public ReadOnlyObjectProperty<Market> selectedMarketProperty() {
        return selectedMarket;
    }

    public Direction getDirection() {
        return direction.get();
    }

    public ReadOnlyObjectProperty<Direction> directionProperty() {
        return direction;
    }

    public Monetary getBaseSideAmount() {
        return baseSideAmount.get();
    }

    public ReadOnlyObjectProperty<Monetary> baseSideAmountProperty() {
        return baseSideAmount;
    }

    public Monetary getQuoteSideAmount() {
        return quoteSideAmount.get();
    }

    public ReadOnlyObjectProperty<Monetary> quoteSideAmountProperty() {
        return quoteSideAmount;
    }

    public Quote getFixPrice() {
        return fixPrice.get();
    }

    public ReadOnlyObjectProperty<Quote> fixPriceProperty() {
        return fixPrice;
    }

    public SwapProtocolType getSelectedProtocolType() {
        return selectedProtocolType.get();
    }

    public ReadOnlyObjectProperty<SwapProtocolType> selectedProtocolTypeProperty() {
        return selectedProtocolType;
    }

    public ObservableSet<Account<? extends SettlementMethod>> getSelectedBaseSideAccounts() {
        return selectedBaseSideAccounts;
    }

    public ObservableSet<Account<? extends SettlementMethod>> getSelectedQuoteSideAccounts() {
        return selectedQuoteSideAccounts;
    }

    public ObservableSet<SettlementMethod> getSelectedBaseSideSettlementMethods() {
        return selectedBaseSideSettlementMethods;
    }

    public ObservableSet<SettlementMethod> getSelectedQuoteSideSettlementMethods() {
        return selectedQuoteSideSettlementMethods;
    }
}
