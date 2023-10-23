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

package bisq.desktop.main.content.trade_apps;

import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.ContentTabModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;

@Getter
public class TradeAppsModel extends ContentTabModel {
    private final BooleanProperty moreTabVisible = new SimpleBooleanProperty();

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.TRADE_PROTOCOLS_OVERVIEW;
    }
}
