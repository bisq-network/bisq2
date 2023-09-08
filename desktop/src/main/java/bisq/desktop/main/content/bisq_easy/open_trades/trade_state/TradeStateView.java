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
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeStateView extends View<VBox, TradeStateModel, TradeStateController> {
    private final Label headline;
    private final HBox phaseAndInfoHBox;
    private final VBox tradeWelcome;
    private Subscription stateInfoVBoxPin;

    public TradeStateView(TradeStateModel model,
                          TradeStateController controller,
                          VBox tradeWelcome,
                          VBox tradePhaseBox) {
        super(new VBox(0), model, controller);
        this.tradeWelcome = tradeWelcome;

        this.root.getStyleClass().addAll("bisq-easy-trade-state-bg");
        this.root.setPadding(new Insets(15, 30, 20, 30));

        headline = new Label();
        headline.getStyleClass().add("bisq-easy-trade-state-headline");

        HBox.setHgrow(tradePhaseBox, Priority.ALWAYS);
        phaseAndInfoHBox = new HBox(tradePhaseBox);

        Region hLine = Layout.hLine();

        VBox.setMargin(hLine, new Insets(0, -30, 0, -30));
        VBox.setMargin(headline, new Insets(2.5, 0, 15, -2));
        VBox.setVgrow(tradeWelcome, Priority.ALWAYS);
        root.getChildren().addAll(headline, hLine, tradeWelcome, phaseAndInfoHBox);
    }

    @Override
    protected void onViewAttached() {
        tradeWelcome.visibleProperty().bind(model.getTradeWelcomeVisible());
        tradeWelcome.managedProperty().bind(model.getTradeWelcomeVisible());
        phaseAndInfoHBox.visibleProperty().bind(model.getPhaseAndInfoBoxVisible());
        phaseAndInfoHBox.managedProperty().bind(model.getPhaseAndInfoBoxVisible());
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
    }

    @Override
    protected void onViewDetached() {
        tradeWelcome.visibleProperty().unbind();
        tradeWelcome.managedProperty().unbind();
        phaseAndInfoHBox.visibleProperty().unbind();
        phaseAndInfoHBox.managedProperty().unbind();
        headline.textProperty().unbind();

        stateInfoVBoxPin.unsubscribe();
    }
}
