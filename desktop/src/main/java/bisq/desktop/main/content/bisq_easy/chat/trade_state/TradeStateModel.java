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

package bisq.desktop.main.content.bisq_easy.chat.trade_state;

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
    @Setter
    private BisqEasyTrade bisqEasyTrade;
    private final ObjectProperty<VBox> stateInfoVBox = new SimpleObjectProperty<>();
    private final BooleanProperty isCollapsed = new SimpleBooleanProperty();
    private final StringProperty headline = new SimpleStringProperty();
    private final BooleanProperty tradeWelcomeVisible = new SimpleBooleanProperty();
    private final BooleanProperty phaseAndInfoBoxVisible = new SimpleBooleanProperty();
}
