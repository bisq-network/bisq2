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

package bisq.desktop.components.table;

import bisq.desktop.components.controls.BisqInputTextField;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.collections.transformation.FilteredList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;


@Slf4j
public class FilterBox {
    private final Controller controller;

    public FilterBox(FilteredList<? extends FilteredListItem> filteredList) {
        controller = new Controller(filteredList);
    }

    public BisqInputTextField getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller(FilteredList<? extends FilteredListItem> filteredList) {
            model = new Model(filteredList);
            view = new View(model, this);
        }

        @Override
        public void onViewAttached() {
        }

        @Override
        public void onViewDetached() {
        }

        private void onSearch(String filterString) {
                model.filteredList.setPredicate(item -> model.defaultPredicate.test(item) && item.match(filterString));
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final FilteredList<? extends FilteredListItem> filteredList;
        private final Predicate<FilteredListItem> defaultPredicate;

        private Model(FilteredList<? extends FilteredListItem> filteredList) {
            this.filteredList = filteredList;
            //noinspection unchecked
            defaultPredicate = (Predicate<FilteredListItem>) filteredList.getPredicate();
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<BisqInputTextField, Model, Controller> {
        private final ChangeListener<String> listener;

        private View(Model model, Controller controller) {
            super(new BisqInputTextField(), model, controller);

            root.setPromptText(Res.get("search"));
            root.setMinWidth(100);

            listener = (observable, oldValue, newValue) -> controller.onSearch(root.getText());
        }

        @Override
        public void onViewAttached() {
            root.textProperty().addListener(listener);
            root.setOnAction(e -> controller.onSearch(root.getText()));
        }

        @Override
        protected void onViewDetached() {
            root.textProperty().removeListener(listener);
        }
    }
}