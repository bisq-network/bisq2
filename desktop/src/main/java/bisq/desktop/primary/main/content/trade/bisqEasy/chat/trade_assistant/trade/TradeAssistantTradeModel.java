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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.trade;

import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.NavigationTarget;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class TradeAssistantTradeModel implements Model {
    private final StringProperty confirmButtonText = new SimpleStringProperty();
    private final BooleanProperty openDisputeButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty topPaneBoxVisible = new SimpleBooleanProperty();
    private final IntegerProperty currentIndex = new SimpleIntegerProperty();
    private final List<NavigationTarget> childTargets = new ArrayList<>();
    private final ObjectProperty<NavigationTarget> selectedChildTarget = new SimpleObjectProperty<>();
}
