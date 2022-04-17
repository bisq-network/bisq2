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

import bisq.account.protocol.ProtocolType;
import bisq.account.protocol.SwapProtocolType;
import bisq.common.monetary.Market;
import javafx.scene.control.Label;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ProtocolSelection {
    private final Controller controller;

    public ProtocolSelection() {
        controller = new Controller();
    }

    public ReadOnlyObjectProperty<SwapProtocolType> selectedProtocolType() {
        return controller.model.selectedProtocolType;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller() {
            model = new Model();
            view = new View(model, this);
        }

        private void setSelectedMarket(Market selectedMarket) {
            if (selectedMarket == null) return;
            model.fillObservableList(ProtocolType.getProtocols(selectedMarket));
            model.selectedProtocolType.set(null);
            model.selectListItem(null);
        }

        private void onSelectProtocol(SwapProtocolType value) {
            model.selectedProtocolType.set(value);
            model.selectListItem(value);
        }

        @Override
        public void onActivate() {
            if (model.selectedMarket != null) {
                model.fillObservableList(ProtocolType.getProtocols(model.selectedMarket));
            }
        }

        @Override
        public void onDeactivate() {
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<SwapProtocolType> selectedProtocolType = new SimpleObjectProperty<>();
        private final ObservableList<ListItem> observableList = FXCollections.observableArrayList();
        private final SortedList<ListItem> sortedList = new SortedList<>(observableList);
        private final ObjectProperty<ListItem> selectedProtocolItem = new SimpleObjectProperty<>();
        private Market selectedMarket;

        private Model() {
        }

        private void fillObservableList(List<SwapProtocolType> protocols) {
            observableList.setAll(protocols.stream().map(ListItem::new).collect(Collectors.toList()));
        }

        private void selectListItem(SwapProtocolType value) {
            observableList.stream().filter(item -> item.protocolType.equals(value)).findAny()
                    .ifPresent(selectedProtocolItem::set);
        }
    }

    @Getter
    private static class ListItem implements TableItem {
        private final SwapProtocolType protocolType;
        private final String protocolName;

        private ListItem(SwapProtocolType protocolType) {
            this.protocolType = protocolType;
            protocolName = Res.get(protocolType.name());
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate() {
        }
    }

    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqTableView<ListItem> tableView;
        private final ChangeListener<ListItem> selectedProtocolItemListener;
        private final ChangeListener<ListItem> selectedTableItemListener;

        private View(Model model,
                     Controller controller) {
            super(new VBox(), model, controller);

            Label headline = new Label(Res.get("createOffer.selectProtocol"));
            headline.getStyleClass().add("titled-group-bg-label-active");

            tableView = new BisqTableView<>(model.sortedList);
            tableView.setFixHeight(130);
            configTableView();

            root.getChildren().addAll(headline, tableView);

            // Listener on table row selection
            selectedTableItemListener = (o, old, newValue) -> {
                if (newValue == null) return;
                controller.onSelectProtocol(newValue.protocolType);
            };

            // Listeners on model change
            selectedProtocolItemListener = (o, old, newValue) -> tableView.getSelectionModel().select(newValue);
        }

        @Override
        protected void onViewAttached() {
            tableView.getSelectionModel().selectedItemProperty().addListener(selectedTableItemListener);
            model.selectedProtocolItem.addListener(selectedProtocolItemListener);
        }

        @Override
        protected void onViewDetached() {
            tableView.getSelectionModel().selectedItemProperty().removeListener(selectedTableItemListener);
            model.selectedProtocolItem.removeListener(selectedProtocolItemListener);
        }

        private void configTableView() {
            tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .title(Res.get("createOffer.protocol.names"))
                    .minWidth(120)
                    .valueSupplier(ListItem::getProtocolName)
                    .build());
            //todo there will be more info about the protocols
        }
    }
}