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

import bisq.desktop.common.view.Model;
import bisq.desktop.components.table.TableList;
import javafx.beans.property.*;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

@Slf4j
@Getter
public class MediatorModel implements Model {
    private final BooleanProperty showClosedCases = new SimpleBooleanProperty();
    private final BooleanProperty noOpenCases = new SimpleBooleanProperty();
    private final StringProperty chatWindowTitle = new SimpleStringProperty();
    private final TableList<MediationCaseListItem> listItems = new TableList<>();
    private final ObjectProperty<MediationCaseListItem> selectedItem = new SimpleObjectProperty<>();
    private final ObjectProperty<Stage> chatWindow = new SimpleObjectProperty<>();
    private final ObjectProperty<Predicate<MediationCaseListItem>> searchPredicate = new SimpleObjectProperty<>(item -> true);
    private final ObjectProperty<Predicate<MediationCaseListItem>> closedCasesPredicate = new SimpleObjectProperty<>(item -> true);

    public MediatorModel() {
    }

    public void reset() {
        // We dont reset showClosedCases
        noOpenCases.setValue(false);
        chatWindowTitle.setValue(null);
        listItems.clear();
        selectedItem.set(null);
        chatWindow.set(null);
        searchPredicate.set(item -> true);
        closedCasesPredicate.set(item -> true);
    }
}
