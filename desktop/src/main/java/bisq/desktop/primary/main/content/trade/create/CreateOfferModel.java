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

package bisq.desktop.primary.main.content.trade.create;

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
import javafx.collections.ObservableSet;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class CreateOfferModel implements Model {
    // References to data in component models
    private ReadOnlyObjectProperty<Market> selectedMarketProperty;
    private ReadOnlyObjectProperty<Direction> directionProperty;
    private ReadOnlyObjectProperty<SwapProtocolType> selectedProtocolTypeProperty;
    private ReadOnlyObjectProperty<Monetary> baseSideAmountProperty;
    private ReadOnlyObjectProperty<Monetary> quoteSideAmountProperty;
    private ReadOnlyObjectProperty<Quote> fixPriceProperty;
    private ObservableSet<Account<? extends SettlementMethod>> selectedBaseSideAccounts;
    private ObservableSet<Account<? extends SettlementMethod>> selectedQuoteSideAccounts;
    private ObservableSet<SettlementMethod> selectedBaseSideSettlementMethods;
    private ObservableSet<SettlementMethod> selectedQuoteSideSettlementMethods;

    private final ObjectProperty<Offer> offerProperty = new SimpleObjectProperty<>();
    private final BooleanProperty createOfferButtonVisibleProperty = new SimpleBooleanProperty(true);
    BooleanProperty showCreateOfferTab = new SimpleBooleanProperty();
    
    public CreateOfferModel() {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Market getSelectedMarket() {
        return selectedMarketProperty.get();
    }

    public Direction getDirection() {
        return directionProperty.get();
    }

    public Monetary getBaseSideAmount() {
        return baseSideAmountProperty.get();
    }

    public Monetary getQuoteSideAmount() {
        return quoteSideAmountProperty.get();
    }

    public Quote getFixPrice() {
        return fixPriceProperty.get();
    }

    public SwapProtocolType getSelectedProtocolType() {
        return selectedProtocolTypeProperty.get();
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

    public boolean isCreateOfferButtonVisible() {
        return createOfferButtonVisibleProperty.get();
    }

    public Offer getOffer() {
        return offerProperty.get();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ReadOnlyObjectProperty
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public ReadOnlyObjectProperty<Market> selectedMarketProperty() {
        return selectedMarketProperty;
    }

    public ReadOnlyObjectProperty<Direction> directionProperty() {
        return directionProperty;
    }

    public ReadOnlyObjectProperty<SwapProtocolType> selectedProtocolTypeProperty() {
        return selectedProtocolTypeProperty;
    }

    public ReadOnlyObjectProperty<Monetary> baseSideAmountProperty() {
        return baseSideAmountProperty;
    }

    public ReadOnlyObjectProperty<Monetary> quoteSideAmountProperty() {
        return quoteSideAmountProperty;
    }

    public ReadOnlyObjectProperty<Quote> fixPriceProperty() {
        return fixPriceProperty;
    }


    public ReadOnlyBooleanProperty createOfferButtonVisibleProperty() {
        return createOfferButtonVisibleProperty;
    }

    public ReadOnlyObjectProperty<Offer> offerProperty() {
        return offerProperty;
    }


}
