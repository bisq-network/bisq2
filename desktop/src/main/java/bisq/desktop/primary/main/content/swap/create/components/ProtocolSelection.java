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

package bisq.desktop.primary.main.content.swap.create.components;

import bisq.account.protocol.ProtocolSwapSettlementMapping;
import bisq.account.protocol.SwapProtocolType;
import bisq.common.monetary.Market;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ProtocolSelection {
    public static class ProtocolController implements Controller {
        private final ProtocolModel model;
        @Getter
        private final ProtocolView view;
        private final ChangeListener<Market> selectedMarketListener;

        public ProtocolController(OfferPreparationModel offerPreparationModel) {
            model = new ProtocolModel(offerPreparationModel);
            view = new ProtocolView(model, this);

            selectedMarketListener = (observable, oldValue, newValue) -> {
                if (newValue == null) return;
                model.fillObservableList(ProtocolSwapSettlementMapping.getProtocols(newValue));
            };
        }

        public void onSelectProtocol(SwapProtocolType value) {
            model.setSelectedProtocolType(value);
            model.selectListItem(value);
        }

        public void onViewAttached() {
            model.selectedMarketProperty().addListener(selectedMarketListener);
            if (model.getSelectedMarket() != null) {
                model.fillObservableList(ProtocolSwapSettlementMapping.getProtocols(model.getSelectedMarket()));
            }
        }

        public void onViewDetached() {
            model.selectedMarketProperty().removeListener(selectedMarketListener);
        }
    }

    private static class ProtocolModel implements Model {
        @Delegate
        private final OfferPreparationModel offerPreparationModel;
        private final ObservableList<ListItem> observableList = FXCollections.observableArrayList();
        private final SortedList<ListItem> sortedList = new SortedList<>(observableList);
        private final ObjectProperty<ListItem> selectedProtocolItem = new SimpleObjectProperty<>();

        public ProtocolModel(OfferPreparationModel offerPreparationModel) {
            this.offerPreparationModel = offerPreparationModel;
        }

        private void fillObservableList(List<SwapProtocolType> protocols) {
            observableList.setAll(protocols.stream().map(ListItem::new).collect(Collectors.toList()));
        }

        public void selectListItem(SwapProtocolType value) {
            observableList.stream().filter(item -> item.protocolType.equals(value)).findAny()
                    .ifPresent(selectedProtocolItem::set);
        }
    }

    @Getter
    private static class ListItem implements TableItem {
        private final SwapProtocolType protocolType;
        private final String protocolName;

        public ListItem(SwapProtocolType protocolType) {
            this.protocolType = protocolType;
            protocolName = Res.offerbook.get(protocolType.name());
        }

        @Override
        public void activate() {
        }

        @Override
        public void deactivate() {
        }
    }

    public static class ProtocolView extends View<VBox, ProtocolModel, ProtocolController> {
        private final BisqTableView<ListItem> tableView;
        private final ChangeListener<ListItem> selectedProtocolItemListener;
        private final ChangeListener<ListItem> selectedTableItemListener;

        public ProtocolView(ProtocolModel model,
                            ProtocolController controller) {
            super(new VBox(), model, controller);

            Label headline = new BisqLabel(Res.offerbook.get("createOffer.selectProtocol"));
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

        public void onViewAttached() {
            tableView.getSelectionModel().selectedItemProperty().addListener(selectedTableItemListener);
            model.selectedProtocolItem.addListener(selectedProtocolItemListener);
        }

        public void onViewDetached() {
            tableView.getSelectionModel().selectedItemProperty().removeListener(selectedTableItemListener);
            model.selectedProtocolItem.removeListener(selectedProtocolItemListener); model.setSelectedProtocolType(null);
        }

        private void configTableView() {
            tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .title(Res.offerbook.get("createOffer.protocol.names"))
                    .minWidth(120)
                    .valueSupplier(ListItem::getProtocolName)
                    .build());
            //todo there will be more info about the protocols
        }
    }
}