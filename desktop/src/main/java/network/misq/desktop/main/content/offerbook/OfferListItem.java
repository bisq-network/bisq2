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

package network.misq.desktop.main.content.offerbook;

import io.reactivex.subjects.BehaviorSubject;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import network.misq.offer.MarketPrice;
import network.misq.offer.Offer;
import network.misq.presentation.offer.OfferEntity;

import java.util.Map;

public class OfferListItem extends OfferEntity {
    @Getter
    private final StringProperty quoteProperty = new SimpleStringProperty("");
    @Getter
    private final StringProperty marketPriceOffsetProperty = new SimpleStringProperty("");
    @Getter
    private final StringProperty quoteAmountProperty = new SimpleStringProperty("");
    @Getter
    private final StringProperty askAmountProperty = new SimpleStringProperty("");
    @Getter
    private final StringProperty bidAmountProperty = new SimpleStringProperty("");
    private BooleanProperty showAllAskCurrencies;
    private BooleanProperty showAllBidCurrencies;

    public OfferListItem(Offer offer,
                         BehaviorSubject<Map<String, MarketPrice>> marketPriceSubject,
                         BooleanProperty showAllAskCurrencies,
                         BooleanProperty showAllBidCurrencies) {

        super(offer, marketPriceSubject);
        this.showAllAskCurrencies = showAllAskCurrencies;
        this.showAllBidCurrencies = showAllBidCurrencies;

        showAllAskCurrencies.addListener(o -> applyAskAmountProperty());
        showAllBidCurrencies.addListener(o -> applyBidAmountProperty());
    }

    @Override
    protected void updatedPriceAndAmount(Map<String, MarketPrice> marketPriceMap) {
        super.updatedPriceAndAmount(marketPriceMap);
        // We get called from the constructor of our superclass, so our fields are not initialized at that moment.
        // We delay with Platform.runLater which guards us also in case we get called from a non JavaFxApplication thread.
        Platform.runLater(() -> {
            applyQuote();
            marketPriceOffsetProperty.set(formattedMarketPriceOffset);

            quoteAmountProperty.set(formattedQuoteAmountWithMinAmount);

            applyBidAmountProperty();
            applyAskAmountProperty();
        });
    }

    private void applyQuote() {
        quoteProperty.set(formattedQuote + getQuoteCode());
    }

    private void applyBidAmountProperty() {
        bidAmountProperty.set(getFormattedBidAmountWithMinAmount() + getBidCurrencyCode());
    }

    private void applyAskAmountProperty() {
        askAmountProperty.set(getFormattedAskAmountWithMinAmount() + getAskCurrencyCode());
    }

    private String getQuoteCode() {
        return showAllBidCurrencies.get() || showAllAskCurrencies.get() ? " " + offer.getQuote().getQuoteCode() : "";
    }

    private String getBidCurrencyCode() {
        return showAllBidCurrencies.get() ? " " + offer.getBidCurrencyCode() : "";
    }

    private String getAskCurrencyCode() {
        return showAllAskCurrencies.get() ? " " + offer.getAskCurrencyCode() : "";
    }
}
