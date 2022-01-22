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

package bisq.desktop.primary.main.content.swap.create;

import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.offer.protocol.SwapProtocolType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
@Getter
public class ProtocolSelection {
    public static class ProtocolController implements Controller {
        private final ProtocolModel model;
        @Getter
        private final ProtocolView view;
        private final ListChangeListener<SwapProtocolType> protocolsChangeListener;

        public ProtocolController(ObservableList<SwapProtocolType> protocols,
                                  ObjectProperty<SwapProtocolType> selectedProtocol) {
            this.model = new ProtocolModel(protocols, selectedProtocol);
            view = new ProtocolView(model, this);

            protocolsChangeListener = c -> model.fillObservableList();
        }

        public void onSelectProtocol(SwapProtocolType value) {
            model.setSelectedProtocolType(value);
        }

        public void onViewAttached() {
            model.protocols.addListener(protocolsChangeListener);
        }

        public void onViewDetached() {
            model.protocols.removeListener(protocolsChangeListener);
        }
    }

    @Getter
    public static class ProtocolModel implements Model {
        private final ObservableList<SwapProtocolType> protocols;
        private final ObservableList<ProtocolItem> observableList = FXCollections.observableArrayList();
        private final SortedList<ProtocolItem> sortedList = new SortedList<>(observableList);
        private final ObjectProperty<ProtocolItem> selectedProtocolItem = new SimpleObjectProperty<>();
        private final ObjectProperty<SwapProtocolType> selectedProtocolType = new SimpleObjectProperty<>();
        public boolean hasFocus;

        public ProtocolModel(ObservableList<SwapProtocolType> protocols,
                             ObjectProperty<SwapProtocolType> selectedProtocolType) {
            this.protocols = protocols;
            fillObservableList();
            setSelectedProtocolType(selectedProtocolType.get());
        }

        private void fillObservableList() {
            observableList.setAll(protocols.stream().map(ProtocolItem::new).collect(Collectors.toList()));
        }

        public void setSelectedProtocolType(SwapProtocolType value) {
            this.selectedProtocolType.set(value);
            observableList.stream().filter(item -> item.protocolType.equals(value)).findAny()
                    .ifPresent(selectedProtocolItem::set);
        }
    }

    @Getter
    private static class ProtocolItem implements TableItem {
        private final SwapProtocolType protocolType;
        private final String protocolName;

        public ProtocolItem(SwapProtocolType protocolType) {
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
        private final BisqTableView<ProtocolItem> tableView;
        private final ChangeListener<ProtocolItem> selectedProtocolItemListener;
        private final ChangeListener<ProtocolItem> selectedTableItemListener;

        public ProtocolView(ProtocolModel model,
                            ProtocolController controller) {
            super(new VBox(), model, controller);

            tableView = new BisqTableView<>(model.getSortedList());
            tableView.setPrefHeight(120);
            configTableView();

            root.setPadding(new Insets(10, 0, 0, 0));
            root.setSpacing(2);
            root.getChildren().addAll(tableView);

            // Listener on table row selection
            selectedTableItemListener = (o, old, newValue) -> {
                if (newValue != null) {
                    controller.onSelectProtocol(newValue.protocolType);
                }
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
            model.selectedProtocolItem.removeListener(selectedProtocolItemListener);
        }

        private void configTableView() {
            tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolItem>()
                    .title(Res.offerbook.get("createOffer.protocol.names"))
                    .minWidth(120)
                    .valueSupplier(ProtocolItem::getProtocolName)
                    .build());
            //todo there will be more info about the protocols
        }
    }
}