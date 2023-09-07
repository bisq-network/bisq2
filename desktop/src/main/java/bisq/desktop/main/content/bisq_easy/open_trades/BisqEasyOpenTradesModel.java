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

package bisq.desktop.main.content.bisq_easy.open_trades;

import bisq.chat.ChatChannelDomain;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.chat.ChatModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class BisqEasyOpenTradesModel extends ChatModel {
    private final ObservableList<BisqEasyOpenTradesView.ListItem> listItems = FXCollections.observableArrayList();
    private final FilteredList<BisqEasyOpenTradesView.ListItem> filteredList = new FilteredList<>(listItems);
    private final SortedList<BisqEasyOpenTradesView.ListItem> sortedList = new SortedList<>(filteredList);
    private final ObjectProperty<BisqEasyOpenTradesView.ListItem> selectedItem = new SimpleObjectProperty<>();

    public BisqEasyOpenTradesModel(ChatChannelDomain chatChannelDomain) {
        super(chatChannelDomain);
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.NONE;
    }
}
