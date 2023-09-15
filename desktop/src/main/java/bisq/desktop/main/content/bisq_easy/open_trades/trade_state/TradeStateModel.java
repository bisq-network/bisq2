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
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
    private BisqEasyTrade bisqEasyTrade;
    @Setter
    private BisqEasyOpenTradeChannel channel;
    private final ObjectProperty<VBox> stateInfoVBox = new SimpleObjectProperty<>();
    private final ObjectProperty<UserProfile> peersUserProfile = new SimpleObjectProperty<>();
    private final StringProperty closeButtonText = new SimpleStringProperty();
    private final StringProperty leftAmount = new SimpleStringProperty();
    private final StringProperty leftAmountDescription = new SimpleStringProperty();
    private final StringProperty rightAmount = new SimpleStringProperty();
    private final StringProperty rightAmountDescription = new SimpleStringProperty();
    private final StringProperty tradeId = new SimpleStringProperty();
    @Setter
    private ReputationScore peersReputationScore;
    @Setter
    private TradeCloseType tradeCloseType;

    void reset() {
        bisqEasyTrade = null;
        channel = null;
        stateInfoVBox.set(null);
        leftAmount.set(null);
        leftAmountDescription.set(null);
        rightAmount.set(null);
        rightAmountDescription.set(null);
        tradeId.set(null);
        peersReputationScore = null;
        tradeCloseType = null;
    }
}
