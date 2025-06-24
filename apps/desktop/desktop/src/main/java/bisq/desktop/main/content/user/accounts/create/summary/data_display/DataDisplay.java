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

package bisq.desktop.main.content.user.accounts.create.summary.data_display;

import bisq.account.accounts.AccountPayload;
import bisq.desktop.common.utils.GridPaneUtil;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

public abstract class DataDisplay<T extends AccountPayload> {
    protected final Controller<T> controller;

    public DataDisplay(T accountPayload) {
        controller = getController(accountPayload);
    }

    protected abstract Controller<T> getController(T accountPayload);

    public GridPane getViewRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    public static abstract class Controller<T extends AccountPayload> implements bisq.desktop.common.view.Controller {
        protected final Model model;
        @Getter
        protected View view;

        protected Controller(T accountPayload) {
            model = createModel(accountPayload);
            view = createView();
        }

        protected abstract View createView();

        protected abstract Model createModel(T accountPayload);

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }
    }

    @Getter
    protected static abstract class Model implements bisq.desktop.common.view.Model {
    }

    @Slf4j
    public static abstract class View extends bisq.desktop.common.view.View<GridPane, Model, Controller<?>> {
        protected static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
        protected static final String VALUE_STYLE = "trade-wizard-review-value";
        protected static final String DETAILS_STYLE = "trade-wizard-review-details";

        int rowIndex = 0;

        protected View(Model model, Controller<?> controller) {
            super(new GridPane(), model, controller);

            root.setHgap(10);
            root.setVgap(10);
            root.setMouseTransparent(true);
            GridPaneUtil.setGridPaneMultiColumnsConstraints(root, 3);
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }


        protected void addDescriptionAndValue(String description, String value, int rowIndex) {
            addDescriptionLabel(description, rowIndex);
            addValueLabel(value, rowIndex);
        }

        protected Label addDescriptionLabel(String description, int rowIndex) {
            Label label = new Label(description);
            label.getStyleClass().add(DESCRIPTION_STYLE);
            root.add(label, 0, rowIndex);
            return label;
        }

        protected Label addValueLabel(String value, int rowIndex) {
            Label label = new Label(value);
            label.getStyleClass().add(VALUE_STYLE);
            root.add(label, 1, rowIndex, 2, 1);
            return label;
        }

        protected Region getLine() {
            Region line = new Region();
            line.setMinHeight(1);
            line.setMaxHeight(1);
            line.setStyle("-fx-background-color: -bisq-border-color-grey");
            line.setPadding(new Insets(9, 0, 8, 0));
            return line;
        }
    }

}
