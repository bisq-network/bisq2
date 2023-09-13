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

import bisq.common.data.Triple;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeStateView extends View<VBox, bisq.desktop.main.content.bisq_easy.open_trades.trade_state.TradeStateModel, bisq.desktop.main.content.bisq_easy.open_trades.trade_state.TradeStateController> {
    private final Label headline;
    private final HBox phaseAndInfoHBox;
    private final Button closeButton;
    private Subscription stateInfoVBoxPin;

    public TradeStateView(bisq.desktop.main.content.bisq_easy.open_trades.trade_state.TradeStateModel model,
                          bisq.desktop.main.content.bisq_easy.open_trades.trade_state.TradeStateController controller,
                          VBox tradePhaseBox) {
        super(new VBox(0), model, controller);

        HBox.setHgrow(tradePhaseBox, Priority.ALWAYS);
        phaseAndInfoHBox = new HBox(tradePhaseBox);

        Triple<Label, HBox, VBox> triple = BisqEasyViewUtils.getContainer("", phaseAndInfoHBox);

        headline = triple.getFirst();

        closeButton = new Button(Res.get("bisqEasy.openTrades.closeTrade"));
        closeButton.setMinWidth(150);
        closeButton.getStyleClass().add("outlined-button");
        triple.getSecond().getChildren().addAll(Spacer.fillHBox(), closeButton);

        VBox vBox = triple.getThird();
        root.getChildren().add(vBox);
    }

    @Override
    protected void onViewAttached() {
        headline.textProperty().bind(model.getHeadline());

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
        headline.textProperty().unbind();

        stateInfoVBoxPin.unsubscribe();

        closeButton.setOnAction(null);
    }
}
