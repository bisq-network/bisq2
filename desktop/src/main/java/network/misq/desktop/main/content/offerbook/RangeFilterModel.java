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

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.transformation.FilteredList;
import lombok.Getter;
import network.misq.presentation.formatters.AmountFormatter;

import java.util.List;
import java.util.function.Predicate;

//todo extract to presentation layer the more generic fields like the predicates
public class RangeFilterModel {
    private final OfferbookModel model;
    @Getter
    private final BooleanProperty visible = new SimpleBooleanProperty();
    @Getter
    private final DoubleProperty lowPercentage = new SimpleDoubleProperty(); // has bidirectional binding
    @Getter
    private final DoubleProperty highPercentage = new SimpleDoubleProperty();// has bidirectional binding
    @Getter
    private final StringProperty lowFormattedAmount = new SimpleStringProperty();
    @Getter
    private final StringProperty highFormattedAmount = new SimpleStringProperty();

    private long min;
    private long max;
    private Predicate<OfferListItem> lowPredicate = e -> true;
    private Predicate<OfferListItem> highPredicate = e -> true;
    private final ChangeListener<Number> lowPercentageListener;
    private final ChangeListener<Number> highPercentageListener;

    public RangeFilterModel(OfferbookModel model) {
        this.model = model;

        lowPercentageListener = (observable, oldValue, newValue) -> {
            long value = lowBaseAmountPercentToValue((double) newValue);
            Predicate<OfferListItem> predicate = item -> item.getOffer().getBaseAsset().amount() >= value;
            setLowBaseAmountPredicate(predicate);
            model.applyListFilterPredicates();
            model.applyBaseCurrency();
        };
        highPercentageListener = (observable, oldValue, newValue) -> {
            long value = highBaseAmountPercentToValue((double) newValue);
            Predicate<OfferListItem> predicate = item -> item.getOffer().getMinBaseAmountOrAmount() <= value;
            setHighBaseAmountPredicate(predicate);
            model.applyListFilterPredicates();
            model.applyBaseCurrency();
        };
    }

    public void initialize() {
        visible.set(true);
    }

    public void activate() {
        applyMinBaseAmountValue();
        applyMaxBaseAmountValue();

        lowPercentage.addListener(lowPercentageListener);
        highPercentage.addListener(highPercentageListener);
    }

    public void deactivate() {
        getLowPercentage().removeListener(lowPercentageListener);
        getHighPercentage().removeListener(highPercentageListener);
    }

    void reset() {
        clearFilterPredicates();
        applyMinBaseAmountValue();
        applyMaxBaseAmountValue();
        lowBaseAmountPercentToValue(0);
        highBaseAmountPercentToValue(100);
        lowPercentage.set(0);
        highPercentage.set(100);
    }

    void clearFilterPredicates() {
        lowPredicate = e -> true;
        highPredicate = e -> true;
    }

    private void applyMinBaseAmountValue() {
        FilteredList<OfferListItem> tempList = new FilteredList<>(model.offerItems);
        tempList.setPredicate(model.askCurrencyPredicate);
        min = getMin(tempList);
    }

    private void applyMaxBaseAmountValue() {
        FilteredList<OfferListItem> tempList = new FilteredList<>(model.offerItems);
        tempList.setPredicate(model.askCurrencyPredicate);
        max = getMax(tempList);
    }

    private long lowBaseAmountPercentToValue(double value) {
        long low = min + Math.round((max - min) * value / 100d);
        lowFormattedAmount.set(AmountFormatter.formatAmount1(low, model.baseCurrency));
        return low;
    }

    private long highBaseAmountPercentToValue(double value) {
        long high = Math.round(max * value / 100d);
        highFormattedAmount.set(AmountFormatter.formatAmount1(high, model.baseCurrency));
        return high;
    }

    private void setLowBaseAmountPredicate(Predicate<OfferListItem> predicate) {
        model.clearFilterPredicates();
        model.listFilterPredicates.add(model.askCurrencyPredicate);
        model.listFilterPredicates.add(predicate);
        model.listFilterPredicates.add(highPredicate);
        lowPredicate = predicate;
        model.applyListFilterPredicates();
    }

    private void setHighBaseAmountPredicate(Predicate<OfferListItem> predicate) {
        model.clearFilterPredicates();
        model.listFilterPredicates.add(model.askCurrencyPredicate);
        model.listFilterPredicates.add(predicate);
        model.listFilterPredicates.add(lowPredicate);
        highPredicate = predicate;
        model.applyListFilterPredicates();
    }

    private static long getMin(List<OfferListItem> offers) {
        return offers.stream()
                .mapToLong(e -> e.getOffer().getMinBaseAmountOrAmount())
                .min()
                .orElse(0);
    }

    private static long getMax(List<OfferListItem> offers) {
        return offers.stream()
                .mapToLong(e -> e.getOffer().getMinBaseAmountOrAmount())//todo shouldn't it be amount?
                .max()
                .orElse(0);
    }
}
