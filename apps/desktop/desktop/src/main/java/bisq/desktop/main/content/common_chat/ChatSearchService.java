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

package bisq.desktop.main.content.common_chat;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Getter
public class ChatSearchService {
    private final StringProperty searchText = new SimpleStringProperty();
    private final SimpleObjectProperty<Runnable> onHelpRequested = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Runnable> onInfoRequested = new SimpleObjectProperty<>();

    public void setOnHelpRequested(Runnable action) {
        onHelpRequested.set(action);
    }

    public void setOnInfoRequested(Runnable action) {
        onInfoRequested.set(action);
    }

    public void triggerHelpRequested() {
        if (onHelpRequested.get() != null) {
            onHelpRequested.get().run();
        }
    }

    public void triggerInfoRequested() {
        if (onInfoRequested.get() != null) {
            onInfoRequested.get().run();
        }
    }
}
