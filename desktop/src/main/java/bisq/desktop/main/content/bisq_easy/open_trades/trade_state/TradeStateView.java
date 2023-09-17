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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state;

import bisq.desktop.common.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeStateView extends View<VBox, TradeStateModel, TradeStateController> {
    private final HBox phaseAndInfoHBox;
    private final Button closeButton;
    private Subscription stateInfoVBoxPin;

    public TradeStateView(TradeStateModel model,
                          TradeStateController controller,
                          VBox tradePhaseBox,
                          HBox tradeDataHeader) {
        super(new VBox(0), model, controller);

        closeButton = new Button();
        closeButton.setMinWidth(160);
        closeButton.getStyleClass().add("outlined-button");

        tradeDataHeader.getChildren().addAll(Spacer.fillHBox(), closeButton);

        HBox.setHgrow(tradePhaseBox, Priority.ALWAYS);
        phaseAndInfoHBox = new HBox(tradePhaseBox);

        VBox.setMargin(phaseAndInfoHBox, new Insets(0, 30, 15, 30));
        VBox vBox = new VBox(tradeDataHeader, Layout.hLine(), phaseAndInfoHBox);
        vBox.getStyleClass().add("bisq-easy-container");


        root.getChildren().add(vBox);
    }

    @Override
    protected void onViewAttached() {
        closeButton.textProperty().bind(model.getCloseButtonText());
        closeButton.visibleProperty().bind(model.getCloseButtonVisible());
        closeButton.managedProperty().bind(model.getCloseButtonVisible());

        stateInfoVBoxPin = EasyBind.subscribe(model.getStateInfoVBox(),
                stateInfoVBox -> {
                    if (phaseAndInfoHBox.getChildren().size() == 2) {
                        phaseAndInfoHBox.getChildren().remove(1);
                    }
                    if (stateInfoVBox != null) {
                        HBox.setHgrow(stateInfoVBox, Priority.ALWAYS);
                        HBox.setMargin(stateInfoVBox, new Insets(20, 0, 0, 0));
                        phaseAndInfoHBox.getChildren().add(stateInfoVBox);
                    }
                });

        closeButton.setOnAction(e -> controller.onCloseTrade());
    }

    @Override
    protected void onViewDetached() {
        closeButton.textProperty().unbind();
        closeButton.visibleProperty().unbind();
        closeButton.managedProperty().unbind();

        stateInfoVBoxPin.unsubscribe();

        closeButton.setOnAction(null);

        if (phaseAndInfoHBox.getChildren().size() == 2) {
            phaseAndInfoHBox.getChildren().remove(1);
        }
    }

}
