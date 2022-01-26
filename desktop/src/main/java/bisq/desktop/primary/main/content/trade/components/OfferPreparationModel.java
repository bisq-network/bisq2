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

package bisq.desktop.primary.main.content.trade.components;

import lombok.extern.slf4j.Slf4j;

/**
 * Shared model for offer preparation
 * Used via Lombok Delegates in components
 * IDE does not recognize that and shows getters incorrectly as unused
 */
@Slf4j
public class OfferPreparationModel {
  //  private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();
   
   

    public OfferPreparationModel() {
    }

/*
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Setters
    ///////////////////////////////////////////////////////////////////////////////////////////////////


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



    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////////////


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
    }*/
}
