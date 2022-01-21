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
import bisq.desktop.components.controls.BisqComboBox;
import bisq.i18n.Res;
import bisq.offer.protocol.SwapProtocolType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
@Getter
public class ProtocolSelection {
    public static class ProtocolSelectionController implements Controller {
        private final ProtocolSelectionModel model;
        @Getter
        private final ProtocolSelectionView view;

        public ProtocolSelectionController(ObservableList<SwapProtocolType> protocols,
                                           ObjectProperty<SwapProtocolType> selectedProtocol) {
            this.model = new ProtocolSelectionModel(protocols, selectedProtocol);
            view = new ProtocolSelectionView(model, this);
        }

        public void onSelectProtocol(SwapProtocolType value) {
            model.selectedProtocol.set(value);
        }
    }

    @Getter
    public static class ProtocolSelectionModel implements Model {
        private final ObservableList<SwapProtocolType> protocols;
        private final ObjectProperty<SwapProtocolType> selectedProtocol;
        public boolean hasFocus;

        public ProtocolSelectionModel(ObservableList<SwapProtocolType> protocols,
                                      ObjectProperty<SwapProtocolType> selectedProtocol) {
            this.protocols = protocols;
            this.selectedProtocol = selectedProtocol;
        }
    }

    public static class ProtocolSelectionView extends View<VBox, ProtocolSelectionModel, ProtocolSelectionController> {
        private final BisqComboBox<SwapProtocolType> comboBox;
        private final ChangeListener<SwapProtocolType> selectedProtocolListener;

        public ProtocolSelectionView(ProtocolSelectionModel model,
                                     ProtocolSelectionController controller) {
            super(new VBox(), model, controller);

            comboBox = new BisqComboBox<>();
            comboBox.setMinHeight(42);
            comboBox.setItems(model.getProtocols());
            comboBox.setMaxWidth(200);
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(@Nullable SwapProtocolType protocolType) {
                    return protocolType != null ? Res.offerbook.get(protocolType.name()) : "";
                }

                @Override
                public SwapProtocolType fromString(String value) {
                    return null;
                }
            });
            comboBox.setVisibleRowCount(10);

            root.setPadding(new Insets(10, 0, 0, 0));
            root.setSpacing(2);
            root.getChildren().addAll(comboBox);

            // Listeners on model change
            selectedProtocolListener = (o, old, newValue) -> comboBox.getSelectionModel().select(newValue);
        }

        public void onViewAttached() {
            comboBox.setOnAction(e -> controller.onSelectProtocol(comboBox.getSelectionModel().getSelectedItem()));
            model.selectedProtocol.addListener(selectedProtocolListener);
        }

        public void onViewDetached() {
            comboBox.setOnAction(null);
            model.selectedProtocol.removeListener(selectedProtocolListener);
        }
    }
}