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

import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.desktop.common.view.Model;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.beans.property.*;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class TradeStateModel implements Model {
    enum TradeCloseType {
        REJECT,
        CANCEL,
        COMPLETED
    }

    @Setter
    private TradeCloseType tradeCloseType;
    private final ObjectProperty<BisqEasyOpenTradeChannel> channel = new SimpleObjectProperty<>();
    private final ObjectProperty<BisqEasyTrade> bisqEasyTrade = new SimpleObjectProperty<>();
    private final ObjectProperty<VBox> stateInfoVBox = new SimpleObjectProperty<>();
    private final StringProperty interruptTradeButtonText = new SimpleStringProperty();
    private final BooleanProperty interruptTradeButtonVisible = new SimpleBooleanProperty(true);
    private final BooleanProperty tradeInterrupted = new SimpleBooleanProperty();
    private final BooleanProperty showReportToMediatorButton = new SimpleBooleanProperty();
    private final StringProperty tradeInterruptedInfo = new SimpleStringProperty();

    void resetAll() {
        reset();
        channel.set(null);
    }

    void reset() {
        tradeCloseType = null;
        bisqEasyTrade.set(null);
        stateInfoVBox.set(null);
        interruptTradeButtonText.set(null);
        interruptTradeButtonVisible.set(true);
        tradeInterrupted.set(false);
        showReportToMediatorButton.set(false);
        tradeInterruptedInfo.set(null);
    }
}
