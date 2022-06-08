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

package bisq.desktop.primary.overlay.onboarding.offer.amount;

import bisq.common.monetary.Coin;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import bisq.presentation.formatters.AmountFormatter;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

public class AmountModel implements Model {
    protected final DoubleProperty exchangeRateProperty = new SimpleDoubleProperty();
    protected final StringProperty quoteCurrencyProperty = new SimpleStringProperty();

    protected final DoubleProperty amountAsDoubleProperty = new SimpleDoubleProperty();
    protected final ObservableValue<Monetary> amountProperty = Bindings.createObjectBinding(
            () -> Coin.asBtc(Math.round(amountAsDoubleProperty.get() * 100000000)),
            amountAsDoubleProperty
    );
    protected final ObservableValue<String> formattedAmountProperty = Bindings.createStringBinding(
            () -> AmountFormatter.formatAmount(amountProperty.getValue(), true),
            amountProperty);
    
    protected final ObservableValue<String> formattedQuoteAmountProperty = Bindings.createStringBinding(
            () -> {
                long quoteAmountAsLong = Math.round(amountAsDoubleProperty.get() * exchangeRateProperty.get() * 10000);
                Monetary xamount = Monetary.from(quoteAmountAsLong, quoteCurrencyProperty.get());
                return "~ " + AmountFormatter.formatAmountWithCode(xamount, true);
            },
            amountAsDoubleProperty, quoteCurrencyProperty, exchangeRateProperty);
    protected final ObservableValue<String> currencyCode = Bindings.createStringBinding(
            () -> amountProperty.getValue().getCode(),
            amountProperty
    );
    
    protected final ObjectProperty<Monetary> minAmountProperty = new SimpleObjectProperty<>();
    protected final ObservableValue<String> formattedMinAmountProperty = Bindings.createStringBinding(
            () -> Res.get("onboarding.amount.minLabel", AmountFormatter.formatAmountWithCode(minAmountProperty.get(), true)),
            minAmountProperty);
    protected final ObservableValue<Number> minAmountAsDouble = Bindings.createDoubleBinding(
            () -> minAmountProperty.get().asDouble(),
            minAmountProperty
    );
        
    protected final ObjectProperty<Monetary> maxAmountProperty = new SimpleObjectProperty<>();
    protected final ObservableValue<String> formattedMaxAmountProperty = Bindings.createStringBinding(
            () -> Res.get("onboarding.amount.maxLabel", AmountFormatter.formatAmountWithCode(maxAmountProperty.get(), true)),
            maxAmountProperty);
    protected final ObservableValue<Number> maxAmountAsDoubleProperty = Bindings.createDoubleBinding(
            () -> maxAmountProperty.get().asDouble(),
            maxAmountProperty
    );
    
    protected final Direction direction;
    
    public String getDirectionAsString() {
        return this.direction == Direction.BUY
                ? Res.get("onboarding.amount.direction.buy")
                : Res.get("onboarding.amount.direction.sell");
    }

    public AmountModel() {
        //todo bind those values into the onboarding process 
        this.direction = Direction.BUY;
        this.quoteCurrencyProperty.set("USD");
        this.exchangeRateProperty.set(30242.20);
        this.amountAsDoubleProperty.set(0.003);
        this.minAmountProperty.set(Coin.asBtc(10000));
        this.maxAmountProperty.set(Coin.asBtc(2000000));
    }
}