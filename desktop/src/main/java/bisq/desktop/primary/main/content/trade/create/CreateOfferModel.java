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
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

@Slf4j
@Getter
public class CreateOfferModel implements Model {
    private final ObjectProperty<Market> selectedMarketProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Direction> directionProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> baseSideAmountProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Monetary> quoteSideAmountProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Quote> fixPriceProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<SwapProtocolType> selectedProtocolTypeProperty = new SimpleObjectProperty<>();
    private final ObservableSet<Account<? extends SettlementMethod>> selectedBaseSideAccounts = FXCollections.observableSet(new HashSet<>());
    private final ObservableSet<Account<? extends SettlementMethod>> selectedQuoteSideAccounts = FXCollections.observableSet(new HashSet<>());
    private final ObservableSet<SettlementMethod> selectedBaseSideSettlementMethods = FXCollections.observableSet(new HashSet<>());
    private final ObservableSet<SettlementMethod> selectedQuoteSideSettlementMethods = FXCollections.observableSet(new HashSet<>());

    private final ObjectProperty<Offer> offerProperty = new SimpleObjectProperty<>();
    private final BooleanProperty createOfferButtonVisibleProperty = new SimpleBooleanProperty(true);

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

    public ReadOnlyObjectProperty<SwapProtocolType> selectedProtocolTypeProperty() {
        return selectedProtocolTypeProperty;
    }

    public ReadOnlyObjectProperty<Direction> directionProperty() {
        return directionProperty;
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
