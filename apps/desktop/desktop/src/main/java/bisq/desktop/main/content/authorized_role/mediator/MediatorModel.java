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

package bisq.desktop.main.content.authorized_role.mediator;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.common.view.Model;
import bisq.desktop.components.table.TableList;
import javafx.beans.property.*;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class MediatorModel implements Model {
    private final Map<String, StringProperty> chatMessagesByChannelId = new HashMap<>();
    private final StringProperty selectedChatMessages = new SimpleStringProperty("");
    private final ObjectProperty<ChatChannel<? extends ChatMessage>> selectedChannel = new SimpleObjectProperty<>();

    private final BooleanProperty showClosedCases = new SimpleBooleanProperty();
    private final BooleanProperty noOpenCases = new SimpleBooleanProperty();
    private final StringProperty chatWindowTitle = new SimpleStringProperty();
    private final TableList<MediatorView.ListItem> listItems = new TableList<>();
    private final ObjectProperty<MediatorView.ListItem> selectedItem = new SimpleObjectProperty<>();
    private final ObjectProperty<Stage> chatWindow = new SimpleObjectProperty<>();

    public MediatorModel() {
    }
}
